package medic.gateway.alert;

import android.content.*;
import android.net.*;
import android.telephony.*;

import static medic.gateway.alert.GatewayLog.*;
import static medic.gateway.alert.SmsCompatibility.*;
import static medic.gateway.alert.Utils.*;

public class MmsIntentProcessor extends BroadcastReceiver {
	public void onReceive(Context ctx, Intent intent) {
		logEvent(ctx, "MmsIntentProcessor.onReceive() :: %s", intent.getAction());

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
			logException(ctx, ex, "MmsIntentProcessor threw exception %s when processing intent: %s",
					ex.getClass(), ex.getMessage());
		}
	}
}
