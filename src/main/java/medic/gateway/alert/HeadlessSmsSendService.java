package medic.gateway.alert;

import android.app.IntentService;
import android.content.Intent;

import static medic.gateway.alert.GatewayLog.logEvent;

public class HeadlessSmsSendService extends IntentService {
	private static final String CLASS_NAME = HeadlessSmsSendService.class.getName();

	public HeadlessSmsSendService() {
		super(CLASS_NAME);
	}

	protected void onHandleIntent(Intent i) {
		logEvent(this, "HeadlessSmsSendService :: received intent.  No action will be taken (none implemented) [Intent: action=%s, data=%s, subject=%s, msg=%s]",
				i.getAction(),
				i.getDataString(),
				i.getStringExtra(Intent.EXTRA_SUBJECT),
				i.getStringExtra(Intent.EXTRA_TEXT));
	}
}
