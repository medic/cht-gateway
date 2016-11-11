package medic.gateway.alert;

import android.content.Context;
import android.content.Intent;

import static java.lang.Integer.toHexString;
import static medic.gateway.alert.GatewayLog.logEvent;
import static medic.gateway.alert.SmsCompatibility.createFromPdu;
import static medic.gateway.alert.WoMessage.Status.DELIVERED;

/**
 * TODO this will currently update the status of any multupart message
 * to the status of the <i>last received part</i>.  More precise support
 * for multipart messages would be helpful, although care would need to
 * be taken to reflect these changes in the API.
 */
@SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.ModifiedCyclomaticComplexity", "PMD.StdCyclomaticComplexity"})
class DeliveryReportHandler {
	/**
	 * Mask for differentiating GSM and CDMA message statuses.
	 * @see https://developer.android.com/reference/android/telephony/SmsMessage.html#getStatus%28%29
	 */
	private static final int GSM_STATUS_MASK = 0xFF;

	private static final WoMessage.Status ANY_STATUS = null;

	private final Context ctx;

//> CONSTRUCTORS
	public DeliveryReportHandler(Context ctx) {
		this.ctx = ctx;
	}

//> PUBLIC API
	public void handle(Intent intent) {
		String id = intent.getStringExtra("id");
		int part = intent.getIntExtra("part", -1);
		logEvent(ctx, "Received delivery report for message %s part %s.", id, part);

		Db db = Db.getInstance(ctx);
		WoMessage m = db.getWoMessage(id);
		if(m == null) {
			logEvent(ctx, "Could not find SMS %s in database for delivery report.", id);
			return;
		}

		int status = createFromPdu(intent).getStatus();

		logEvent(ctx, "Delivery status: 0x" + toHexString(status));

		if((status & GSM_STATUS_MASK) == status) {
			handleGsmDelivery(ctx, status, m);
		} else {
			handleCdmaDelivery(ctx);
		}
	}

//> INTERNAL METHODS
	/**
	 * Decode the status value as per ETSI TS 123 040 V13.1.0 (2016-04) 9.2.3.15 (TP-Status (TP-ST)).
	 * @see http://www.etsi.org/deliver/etsi_ts/123000_123099/123040/13.01.00_60/ts_123040v130100p.pdf
	 */
	@SuppressWarnings("PMD.EmptyIfStmt")
	private void handleGsmDelivery(Context ctx, int status, WoMessage m) {
		// Detail of the failure.  Must be set for FAILED messages.
		String fDetail = null;

		Db db = Db.getInstance(ctx);

		if(status < 0x20) {
			//> Short message transaction completed
			switch(status) {
				case 0x00: //> Short message received by the SME
				case 0x01: //> Short message forwarded by the SC to the SME but the SC is unable to confirm delivery
					db.updateStatus(m, ANY_STATUS, DELIVERED);
					return;
				case 0x02: // Short message replaced by the SC
					// Not sure what to do with this.
			}
			if(status < 0x10) {
				// These values are "reserved"
			} else {
				//> Values specific to each SC
			}
			// For now, we will just ignore statuses that we don't understand.
			return;
		} else if(status < 0x40) {
			//> Temporary error, SC still trying to transfer SM
			// no need to report this status yet
			return;
		} else if(status < 0x60) {
			//> Permanent error, SC is not making any more transfer attempts
			switch(status) {
				case 0x40: fDetail = "Remote procedure error"; break;
				case 0x41: fDetail = "Incompatible destination"; break;
				case 0x42: fDetail = "Connection rejected by SME"; break;
				case 0x43: fDetail = "Not obtainable"; break;
				case 0x44: fDetail = "Quality of service not available"; break;
				case 0x45: fDetail = "No interworking available"; break;
				case 0x46: fDetail = "SM Validity Period Expired"; break;
				case 0x47: fDetail = "SM Deleted by originating SME"; break;
				case 0x48: fDetail = "SM Deleted by SC Administration"; break;
				case 0x49: fDetail = "SM does not exist"; break;
				default:
					if(status < 0x50) fDetail = String.format("Permanent error (Reserved: 0x%s)", toHexString(status));
					else fDetail = "SMSC-specific permanent error: 0x" + toHexString(status);
			}
		} else if(status <= 0x7f) {
			//> Temporary error, SC is not making any more transfer attempts
			switch(status) {
				case 0x60: fDetail = "Congestion"; break;
				case 0x61: fDetail = "SME busy"; break;
				case 0x62: fDetail = "No response from SME"; break;
				case 0x63: fDetail = "Service rejected"; break;
				case 0x64: fDetail = "Quality of service not available"; break;
				case 0x65: fDetail = "Error in SME"; break;
				default:
					if(status < 0x70) fDetail = String.format("Temporary error (Reserved: 0x%s)", toHexString(status));
					else fDetail = "SMSC-specific temporary error: 0x" + toHexString(status);
			}
		} else throw new IllegalStateException("Unexpected status (> 0x7F) : 0x" + toHexString(status));

		db.setFailed(m, "Delivery failed: " + fDetail);
		logEvent(ctx, "Delivering message to %s failed (cause: %s)", m.to, fDetail);
	}

	private void handleCdmaDelivery(Context ctx) {
		logEvent(ctx, "Delivery reports not yet supported on CDMA devices.");
	}
}
