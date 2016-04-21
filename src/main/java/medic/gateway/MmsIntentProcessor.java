package medic.gateway;

import android.content.*;
import android.net.*;
import android.telephony.*;

import static android.app.Activity.RESULT_OK;
import static android.provider.Telephony.Sms.Intents.*;
import static medic.gateway.BuildConfig.DEBUG;
import static medic.gateway.DebugLog.logEvent;
import static medic.gateway.Utils.*;

public class MmsIntentProcessor extends BroadcastReceiver {
	public void onReceive(Context ctx, Intent intent) {
		logEvent(ctx, "MmsIntentProcessor.onReceive() :: " + intent.getAction());

		try {
			switch(intent.getAction()) {
				case WAP_PUSH_DELIVER_ACTION:
					// We will receive WAP_PUSH_DELIVER_ACTION on Android 4.4+ if set as the
					// default SMS application.
					// TODO store MMS/WAP Push to the normal inbox
					break;
				default:
					throw new IllegalStateException("Unexpected intent: " + intent);
			}
		} catch(Exception ex) {
			if(DEBUG) ex.printStackTrace();
			logEvent(ctx, "MmsIntentProcessor threw exception %s when processing intent: %s",
					ex.getClass(), ex.getMessage());
		}
	}

	private void log(String message, Object...extras) {
		if(DEBUG) System.err.println("LOG | MmsIntentProcessor :: " +
				String.format(message, extras));
	}
}
