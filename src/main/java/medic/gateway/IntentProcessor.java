package medic.gateway;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsMessage;

import java.io.*;
import java.util.*;

import static android.app.Activity.RESULT_OK;
import static android.telephony.SmsManager.RESULT_ERROR_GENERIC_FAILURE;
import static android.telephony.SmsManager.RESULT_ERROR_NO_SERVICE;
import static android.telephony.SmsManager.RESULT_ERROR_NULL_PDU;
import static android.telephony.SmsManager.RESULT_ERROR_RADIO_OFF;
import static medic.gateway.GatewayLog.logEvent;
import static medic.gateway.GatewayLog.logException;
import static medic.gateway.SmsCompatibility.getMessagesFromIntent;
import static medic.gateway.SmsCompatibility.SMS_DELIVER_ACTION;
import static medic.gateway.SmsCompatibility.SMS_RECEIVED_ACTION;
import static medic.gateway.WoMessage.Status.PENDING;
import static medic.gateway.WoMessage.Status.SENT;

public class IntentProcessor extends BroadcastReceiver {
	static final String SENDING_REPORT = "medic.gateway.SENDING_REPORT";
	static final String DELIVERY_REPORT = "medic.gateway.DELIVERY_REPORT";

	private final Capabilities app;

	public IntentProcessor() {
		super();

		this.app = Capabilities.getCapabilities();
	}

	public void onReceive(Context ctx, Intent intent) {
		logEvent(ctx, "IntentProcessor.onReceive() :: %s", intent.getAction());

		try {
			switch(intent.getAction()) {
				case SMS_RECEIVED_ACTION:
					if(app.canBeDefaultSmsProvider() && app.isDefaultSmsProvider(ctx)) {
						// on Android 4.4+ (kitkat), we will receive both SMS_RECEIVED_ACTION
						// _and_ SMS_DELIVER_ACTION if we are the default SMS app.  Ignoring.
						break;
					}
				case SMS_DELIVER_ACTION:
					handleSmsReceived(ctx, intent);
					break;
				case SENDING_REPORT:
					handleSendingReport(ctx, intent);
					break;
				case DELIVERY_REPORT:
					new DeliveryReportHandler(ctx).handle(intent);
					break;
				default:
					throw new IllegalStateException("Unexpected intent: " + intent);
			}
		} catch(Exception ex) {
			logException(ctx, ex,
					"IntentProcessor threw exception '%s' when processing intent: %s",
					ex.getClass(), ex.getMessage());
		}
	}

	@SuppressWarnings("PMD.UseConcurrentHashMap")
	private void handleSmsReceived(Context ctx, Intent intent) {
		Db db = Db.getInstance(ctx);

		Map<String, MultipartSms> multipartMessages = new HashMap<>();

		for(SmsMessage m : getMessagesFromIntent(intent)) {
			MultipartData multipart = MultipartData.from(m);

			if(multipart != null) {
				String lookupKey = String.format("%s:%s", multipart.ref, m.getOriginatingAddress());
				if(!multipartMessages.containsKey(lookupKey)) {
					multipartMessages.put(lookupKey, new MultipartSms(m, multipart));
				} else {
					multipartMessages.get(lookupKey).add(m, multipart);
				}
			} else {
				boolean success = db.store(m);
				if(!success) {
					logEvent(ctx, "Failed to save received SMS to db: %s", m);
				}
			}
		}

		for(MultipartSms m : multipartMessages.values()) {
			boolean success = db.store(m);
			if(!success) {
				logEvent(ctx, "Failed to save receive multipart SMS to db: %s", m);
			}
		}

		// android >= 1.6 && android < 4.4: SMS_RECEIVED_ACTION is an
		// ordered broadcast, so if we cancel it then it should never
		// reach the inbox.  On 4.4+, either (a) medic-gateway is the
		// default SMS app, so the SMS will never reach the standard
		// inbox, or (b) it is _not_ the default SMS app, in which case
		// there is no way to delete the message.
		abortBroadcast();
	}

	private void handleSendingReport(Context ctx, Intent intent) {
		String id = intent.getStringExtra("id");
		int part = intent.getIntExtra("part", -1);
		logEvent(ctx, "Received delivery report for message %s part %s.", id, part);

		Db db = Db.getInstance(ctx);
		WoMessage m = db.getWoMessage(id);
		if(m == null) {
			logEvent(ctx, "Could not find SMS %s in database for sending report.", id);
		} else {
			int resultCode = getResultCode();
			switch(resultCode) {
				case RESULT_OK:
					db.updateStatus(m, PENDING, SENT);
					break;
				case RESULT_ERROR_GENERIC_FAILURE:
					int errorCode = intent.getIntExtra("errorCode", -1);
					db.setFailed(m, "generic:" + errorCode);
					break;
				case RESULT_ERROR_NO_SERVICE:
					db.setFailed(m, "no-service");
					break;
				case RESULT_ERROR_NULL_PDU:
					db.setFailed(m, "null-pdu");
					break;
				case RESULT_ERROR_RADIO_OFF:
					db.setFailed(m, "radio-off");
					break;
				default:
					db.setFailed(m, "unknown:" + resultCode);
			}
		}
	}
}
