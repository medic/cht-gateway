package medic.gateway.alert;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony.Sms.Intents;
import android.view.View;

import static medic.gateway.alert.GatewayLog.trace;
import static medic.gateway.alert.Utils.getAppName;
import static medic.gateway.alert.Utils.setText;

public class PromptToSetAsDefaultMessageAppActivity extends Activity {

	private static final int REQUEST_CHANGE_DEFAULT_MESSAGING_APP = 1;
	private final Capabilities app;

	public PromptToSetAsDefaultMessageAppActivity() {
		super();

		this.app = Capabilities.getCapabilities();
	}

//> EVENT HANDLERS

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		log("Starting view for PromptToSetAsDefaultMessageAppActivity...");

		setContentView(R.layout.set_as_default_messaging_app);
		String appName = getAppName(this);
		setText(this, R.id.txtDefaultMessageAppWarning, R.string.txtDefaultMessageAppWarning, appName);
		setText(this, R.id.txtDefaultMessageAppPrompt, R.string.txtDefaultMessageAppPrompt, appName);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_CHANGE_DEFAULT_MESSAGING_APP:
				// we should now know if we're the default SMS app from the value of
				// resultCode, but it seems a little odd to trust that result when we
				// can just check a method.
				if(app.isDefaultSmsProvider(this)) {
					continueToSettings();
				}
				break;
			default:
				log("PromptToSetAsDefaultMessageAppActivity :: onActivityResult() :: No handling for requestCode: %s", requestCode);
		}
	}

//> CUSTOM EVENT HANDLERS

	public void dismissActivity(View view) {
		continueToSettings();
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	@SuppressLint({"ObsoleteSdkInt", "InlinedApi"})
	public void openDefaultMessageAppSettings(View view) {
		log("Trying to open SMS Dialog requesting default app. SDK: %s", Build.VERSION.SDK_INT);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
			startIntentToSetSMSRoleHolder();
			return;
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			Intent intent = new Intent(Intents.ACTION_CHANGE_DEFAULT)
					.putExtra(Intents.EXTRA_PACKAGE_NAME, getPackageName());
			startActivityForResult(intent, REQUEST_CHANGE_DEFAULT_MESSAGING_APP);
			return;
		}
	}

	@TargetApi(Build.VERSION_CODES.Q)
	@SuppressLint({"ObsoleteSdkInt", "InlinedApi"})
	void startIntentToSetSMSRoleHolder() {
		RoleManager roleManager = getSystemService(RoleManager.class);

		if (!roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
			log("SMS Role is not available in the system. Check the phone settings.");
			return;
		}


		if (roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
			log("Gateway is already the default app for SMS.");
			return;
		}

		Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS);
		startActivityForResult(intent, REQUEST_CHANGE_DEFAULT_MESSAGING_APP);
	}

//> PRIVATE HELPERS

	private void continueToSettings() {
		log("Navigating to Settings View.");
		startActivity(new Intent(this, SettingsDialogActivity.class));
		finish();
	}

	private void log(String message, Object... extras) {
		trace(this, message, extras);
	}
}
