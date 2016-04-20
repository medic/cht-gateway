package medic.gateway;

import android.content.*;
import android.net.*;
import android.telephony.*;

import static android.provider.Telephony.Sms.Intents.*;
import static medic.gateway.BuildConfig.DEBUG;
import static medic.gateway.DebugLog.logEvent;
import static medic.gateway.Utils.*;

public class IntentProcessor extends BroadcastReceiver {
	private static final Uri SMS_INBOX = Uri.parse("content://sms/inbox");

	public void onReceive(Context ctx, Intent intent) {
		logEvent(ctx, "IntentProcessor.onReceive() :: " + intent.getAction());

		if(DEBUG) System.err.println("###############################");
		if(DEBUG) System.err.println("# IntentProcessor.onReceive() #");
		if(DEBUG) System.err.println("# intent: " + intent);
		if(DEBUG) System.err.println("###############################");

		try {
			if(intent.getAction().equals(SMS_RECEIVED_ACTION)) {
				handleSmsReceived(ctx, intent);
			} else throw new IllegalStateException("Unexpected intent: " + intent);
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

	private void log(String message, Object...extras) {
		if(DEBUG) System.err.println("LOG | IntentProcessor :: " +
				String.format(message, extras));
	}
}
