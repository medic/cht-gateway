package medic.gateway;

import android.content.*;
import android.telephony.*;

import static medic.gateway.BuildConfig.DEBUG;
import static medic.gateway.DebugLog.logEvent;
import static android.provider.Telephony.Sms.Intents.*;

public class IntentProcessor extends BroadcastReceiver {
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
			if(!success) logEvent(ctx, "Failed to save received SMS to db: " + m);
		}
	}

	private void log(String message, Object...extras) {
		if(DEBUG) System.err.println("LOG | IntentProcessor :: " +
				String.format(message, extras));
	}
}
