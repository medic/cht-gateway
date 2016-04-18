package medic.gateway;

import android.content.*;
import android.telephony.*;

import static medic.gateway.BuildConfig.DEBUG;
import static medic.gateway.DebugLog.logEvent;
import static android.provider.Telephony.Sms.Intents.*;

public class IntentProcessor extends BroadcastReceiver {
	private WtRepo wtRepo = WtRepo.$;

	public void onReceive(Context ctx, Intent intent) {
		logEvent("IntentProcessor.onReceive() :: " + intent.getAction());

		System.err.println("###############################");
		System.err.println("# IntentProcessor.onReceive() #");
		System.err.println("# intent: " + intent);
		System.err.println("###############################");

		try {
			if(intent.getAction().equals(SMS_RECEIVED_ACTION)) {
				handleSmsReceived(intent);
			} else if(intent.getAction().equals("medic.gateway.ACTION_POLL")) {
				new ServiceManager(ctx).run();
			} else throw new IllegalStateException("Unexpected intent: " + intent);
		} catch(Exception ex) {
			if(DEBUG) ex.printStackTrace();
		}
	}

	private void handleSmsReceived(Intent intent) {
		for(SmsMessage m : getMessagesFromIntent(intent)) {
			wtRepo.save(m);
		}
	}

	private void log(String message, Object...extras) {
		if(DEBUG) System.err.println("LOG | IntentProcessor :: " +
				String.format(message, extras));
	}
}
