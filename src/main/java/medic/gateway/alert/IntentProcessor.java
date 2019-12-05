package medic.gateway.alert;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsMessage;

import org.json.JSONException;

import java.net.MalformedURLException;

import static android.app.Activity.RESULT_OK;
import static android.telephony.SmsManager.RESULT_ERROR_GENERIC_FAILURE;
import static android.telephony.SmsManager.RESULT_ERROR_NO_SERVICE;
import static android.telephony.SmsManager.RESULT_ERROR_NULL_PDU;
import static android.telephony.SmsManager.RESULT_ERROR_RADIO_OFF;
import static medic.gateway.alert.GatewayLog.logEvent;
import static medic.gateway.alert.GatewayLog.logException;
import static medic.gateway.alert.SmsCompatibility.getMessagesFromIntent;
import static medic.gateway.alert.SmsCompatibility.SMS_DELIVER_ACTION;
import static medic.gateway.alert.SmsCompatibility.SMS_RECEIVED_ACTION;
import static medic.gateway.alert.WoMessage.Status.PENDING;
import static medic.gateway.alert.WoMessage.Status.SENT;

public class IntentProcessor extends BroadcastReceiver {
	static final String SENDING_REPORT = "medic.gateway.alert.SENDING_REPORT";
	static final String DELIVERY_REPORT = "medic.gateway.alert.DELIVERY_REPORT";

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
					new SendingReportHandler(ctx).handle(intent, getResultCode());
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

		for(SmsMessage m : getMessagesFromIntent(intent)) {
			boolean success = db.store(m);

			if(!success) {
				logEvent(ctx, "Failed to save received SMS to db: %s", m);
			}
		}

		try {
			WebappPoller poller = new WebappPoller(ctx);
			SimpleResponse lastResponse = poller.pollWebapp();

			if(lastResponse == null || lastResponse.isError()) {
				LastPoll.failed(ctx);
			} else {
				LastPoll.succeeded(ctx);
			}
		} catch(Exception ex) {
			logException(ctx, ex, "Exception caught trying to poll webapp: %s", ex.getMessage());
			LastPoll.failed(ctx);
		} finally {
			LastPoll.broadcast(ctx);
		}

		// android >= 1.6 && android < 4.4: SMS_RECEIVED_ACTION is an
		// ordered broadcast, so if we cancel it then it should never
		// reach the inbox.  On 4.4+, either (a) medic-gateway is the
		// default SMS app, so the SMS will never reach the standard
		// inbox, or (b) it is _not_ the default SMS app, in which case
		// there is no way to delete the message.
		abortBroadcast();
	}
}

class SendingReportHandler {
	private final Context ctx;

	SendingReportHandler(Context ctx) {
		this.ctx = ctx;
	}

	void handle(Intent intent, int resultCode) {
		String id = intent.getStringExtra("id");
		int part = intent.getIntExtra("part", -1);
		logEvent(ctx, "Received sending report for message %s part %s.", id, part);

		Db db = Db.getInstance(ctx);
		WoMessage m = db.getWoMessage(id);
		if(m == null) {
			logEvent(ctx, "Could not find SMS %s in database for sending report.", id);
		} else if(resultCode == RESULT_OK) {
			db.updateStatus(m, PENDING, SENT);
		} else {
			String failureReason;
			switch(resultCode) {
				case RESULT_ERROR_GENERIC_FAILURE:
					failureReason = getGenericFailureReason(intent);
					break;
				case RESULT_ERROR_NO_SERVICE:
					failureReason = "no-service";
					break;
				case RESULT_ERROR_NULL_PDU:
					failureReason = "null-pdu";
					break;
				case RESULT_ERROR_RADIO_OFF:
					failureReason = "radio-off";
					break;
				default:
					failureReason = "unknown; resultCode=" + resultCode;
			}
			db.setFailed(m, failureReason);
			logEvent(ctx, "Sending message to %s failed (cause: %s)", m.to, failureReason);
		}
	}

	private String getGenericFailureReason(Intent intent) {
		if(intent.hasExtra("errorCode")) {
			int errorCode = intent.getIntExtra("errorCode", -1);
			return "generic; errorCode=" + errorCode;
		} else {
			return "generic; no errorCode supplied";
		}
	}
}
