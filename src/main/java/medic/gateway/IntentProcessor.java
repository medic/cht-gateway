package medic.gateway;

import android.content.*;
import android.net.*;
import android.telephony.*;

import static android.app.Activity.RESULT_OK;
import static android.provider.Telephony.Sms.Intents.*;
import static medic.gateway.GatewayLog.*;
import static medic.gateway.Utils.*;
import static medic.gateway.WoMessage.Status.*;

public class IntentProcessor extends BroadcastReceiver {
	private static final Uri SMS_INBOX = android.provider.Telephony.Sms.Inbox.CONTENT_URI;
	static final String SENDING_REPORT = "medic.gateway.SENDING_REPORT";
	static final String DELIVERY_REPORT = "medic.gateway.DELIVERY_REPORT";

	public void onReceive(Context ctx, Intent intent) {
		logEvent(ctx, "IntentProcessor.onReceive() :: " + intent.getAction());

		try {
			switch(intent.getAction()) {
				case SMS_RECEIVED_ACTION:
					if(!isDefaultSmsProvider(ctx)) {
						// on Android 4.4+ (kitkat), we will receive both SMS_RECEIVED_ACTION
						// _and_ SMS_DELIVER_ACTION if we are the default SMS app.  Ignoring
						break;
					}
				case SMS_DELIVER_ACTION:
					handleSmsReceived(ctx, intent);
					break;
				case SENDING_REPORT:
					handleSendingReport(ctx, intent);
					break;
				case DELIVERY_REPORT:
					handleDeliveryReport(ctx, intent);
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

	private void handleSmsReceived(Context ctx, Intent intent) {
		Db db = Db.getInstance(ctx);
		for(SmsMessage m : getMessagesFromIntent(intent)) {
			boolean success = db.store(m);
			if(success) {
				deleteSmsFromDeviceInbox(ctx, m);
			} else {
				logEvent(ctx, "Failed to save received SMS to db: " + m);
			}
		}
	}

	/**
	 * Delete a message from the device's SMS Inbox.
	 *
	 * On Android 4.4+, this will fail silently.  This is not really an
	 * issue - proper users should have medic-gateway set as the default
	 * SMS application anyway, in which case the messages will never reach
	 * the device's inbox in the first place.
	 */
	private void deleteSmsFromDeviceInbox(Context ctx, SmsMessage sms) {
		int rowsDeleted = ctx.getContentResolver().delete(SMS_INBOX, "address=? AND date=? AND body=?",
				args(sms.getOriginatingAddress(), sms.getTimestampMillis(), sms.getMessageBody()));
		logEvent(ctx, "Attempted to delete %s; %s messages deleted.", sms, rowsDeleted);
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
				default:
					db.updateStatus(m, PENDING, FAILED);
			}
		}
	}

	private void handleDeliveryReport(Context ctx, Intent intent) {
		String id = intent.getStringExtra("id");
		int part = intent.getIntExtra("part", -1);
		logEvent(ctx, "Received delivery report for message %s part %s.", id, part);

		Db db = Db.getInstance(ctx);
		WoMessage m = db.getWoMessage(id);
		if(m == null) {
			logEvent(ctx, "Could not find SMS %s in database for delivery report.", id);
		} else {
			db.updateStatus(m, SENT, DELIVERED);
		}
	}
}
