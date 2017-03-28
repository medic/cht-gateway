package medic.gateway.alert;

import android.Manifest.permission;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.View;

import static medic.gateway.alert.Utils.getAppName;
import static medic.gateway.alert.Utils.setText;
import static medic.gateway.alert.Utils.startSettingsOrMainActivity;

/**
 * To support Android 6.0+ (marshmallow), we must request SMS permissions at
 * runtime as well as in {@code AndroidManifest.xml}.
 * @see https://developer.android.com/intl/ru/about/versions/marshmallow/android-6.0-changes.html#behavior-runtime-permissions
 */
public class PromptForSmsPermissionsActivity extends Activity implements ActivityCompat.OnRequestPermissionsResultCallback {
	private static final String[] DESIRED_PERMISSIONS =
			{ permission.SEND_SMS, permission.RECEIVE_SMS };

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.prompt_for_permissions);

		setText(this, R.id.txtPermissionsPrompt, R.string.txtPermissionsPrompt, getAppName(this));
	}

	public void triggerPermissionRequest(View v) {
		ActivityCompat.requestPermissions(this, DESIRED_PERMISSIONS, 0);
	}

	@SuppressWarnings("PMD.UseVarargs")
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		// Whatever permissions the user granted (or failed to), we will carry on regardless.
		// If some permissions are lacking, the warning will be displayed again next time the app is started.
		startSettingsOrMainActivity(this);
		finish();
	}
}
