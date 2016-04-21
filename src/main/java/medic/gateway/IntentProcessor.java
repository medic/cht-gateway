package medic.gateway;

import android.content.*;
import android.net.*;
import android.telephony.*;

import static android.app.Activity.RESULT_OK;
import static android.provider.Telephony.Sms.Intents.*;
import static medic.gateway.BuildConfig.DEBUG;
import static medic.gateway.DebugLog.logEvent;
import static medic.gateway.Utils.*;

public class IntentProcessor extends BroadcastReceiver {
	private static final Uri SMS_INBOX = android.provider.Telephony.Sms.Inbox.CONTENT_URI;
	static final String SENDING_REPORT = "medic.gateway.SENDING_REPORT";
	static final String DELIVERY_REPORT = "medic.gateway.DELIVERY_REPORT";

	public void onReceive(Context ctx, Intent intent) {
		logEvent(ctx, "IntentProcessor.onReceive() :: " + intent.getAction());

		if(DEBUG) System.err.println("###############################");
		if(DEBUG) System.err.println("# IntentProcessor.onReceive() #");
		if(DEBUG) System.err.println("# intent: " + intent);
		if(DEBUG) System.err.println("###############################");

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
			if(DEBUG) ex.printStackTrace();
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
		} else if(m.getStatus() == WoMessage.Status.PENDING) {
			int resultCode = getResultCode();
			switch(resultCode) {
				case RESULT_OK:
					m.setStatus(WoMessage.Status.SENT);
					break;
				default:
					m.setStatus(WoMessage.Status.FAILED);
			}
			logEvent(ctx, "Updating SMS %s to status %s (result code %s).", id, m.getStatus(), resultCode);
			// TODO should use a more specific update, where we match the id, status and lastAction when
			// choosing which row to update, and only changing status and lastAction fields
			db.update(m);
		} else {
			logEvent(ctx, "Not updating SMS %s for sent report, because current status is %s.", id, m.getStatus());
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
		} else if(m.getStatus() == WoMessage.Status.SENT) {
			m.setStatus(WoMessage.Status.DELIVERED);
			logEvent(ctx, "Updating SMS %s to status %s.", id, m.getStatus());
			// TODO should use a more specific update, where we match the id, status and lastAction when
			// choosing which row to update, and only changing status and lastAction fields
			db.update(m);
		} else {
			logEvent(ctx, "Not updating SMS %s for sent report, because current status is %s.", id, m.getStatus());
		}
	}

	private void log(String message, Object...extras) {
		if(DEBUG) System.err.println("LOG | IntentProcessor :: " +
				String.format(message, extras));
	}
}
