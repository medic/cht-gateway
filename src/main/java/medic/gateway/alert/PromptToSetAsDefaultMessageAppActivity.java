package medic.gateway.alert;

import android.app.Activity;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony.Sms.Intents;
import android.view.View;

import static medic.gateway.alert.GatewayLog.logEvent;
import static medic.gateway.alert.GatewayLog.trace;
import static medic.gateway.alert.Utils.getAppName;
import static medic.gateway.alert.Utils.setText;

@TargetApi(19)
public class PromptToSetAsDefaultMessageAppActivity extends Activity {
	private static final int REQUEST_CHANGE_DEFAULT_MESSAGING_APP = 1;

	private final Capabilities app;

	public PromptToSetAsDefaultMessageAppActivity() {
		super();

		this.app = Capabilities.getCapabilities();
	}

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		log("Starting...");

		String currentDefaultSmsApp = android.provider.Settings.Secure.getString(getContentResolver(),
				// this string is copied from android.provider.Settings.Secure.SMS_DEFAULT_APPLICATION
				// because directly referencing this constant throws a compile error, presumably
				// because of legacy support.
				"sms_default_application");
		log("Current default SMS app is %s", currentDefaultSmsApp);

		setContentView(R.layout.set_as_default_messaging_app);

		setText(this, R.id.txtDefaultMessageAppPrompt, R.string.txtDefaultMessageAppPrompt, getAppName(this));
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
			case REQUEST_CHANGE_DEFAULT_MESSAGING_APP:
				logEvent(this, "onActivityResult() :: returned from SMS app settings.  resultCode=%s, data=%s", resultCode, data);
				// we should now know if we're the default SMS app from the value of
				// resultCode, but it seems a little odd to trust that result when we
				// can just check a method.
				if(app.isDefaultSmsProvider(this)) {
					continueToSettings();
				}
				break;
			default: trace(this, "onActivityResult() :: No handling for requestCode: %s", requestCode);
		}
	}

//> EVENT HANDLERS
	public void dismissActivity(View v) {
		continueToSettings();
	}

	public void openDefaultMessageAppSettings(View v) {
		Intent i = new Intent(Intents.ACTION_CHANGE_DEFAULT);
		i.putExtra(Intents.EXTRA_PACKAGE_NAME, getPackageName());
		startActivityForResult(i, REQUEST_CHANGE_DEFAULT_MESSAGING_APP);
	}

//> PRIVATE HELPERS
	private void continueToSettings() {
		startActivity(new Intent(this, SettingsDialogActivity.class));
		finish();
	}

	private void log(String message, Object... extras) {
		trace(this, message, extras);
	}
}
