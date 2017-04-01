package medic.gateway.alert;

import android.Manifest.permission;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static medic.gateway.alert.Utils.getAppName;
import static medic.gateway.alert.Utils.setText;
import static medic.gateway.alert.Utils.startSettingsOrMainActivity;

/**
 * To support Android 6.0+ (marshmallow), we must request SMS permissions at
 * runtime as well as in {@code AndroidManifest.xml}.
 * @see https://developer.android.com/intl/ru/about/versions/marshmallow/android-6.0-changes.html#behavior-runtime-permissions
 */
public class PromptForPermissionsActivity extends Activity implements ActivityCompat.OnRequestPermissionsResultCallback {
	private static final String X_PERMISSIONS_TYPE = "permissionsType";
	private static final Object[][] PERMISSIONS_REQUESTS = {
		/* sms */ { R.string.txtPermissionsPrompt_sms, new String[] { permission.SEND_SMS, permission.RECEIVE_SMS } },
		/* file access */ { R.string.txtPermissionsPrompt_fileAccess, new String[] { permission.WRITE_EXTERNAL_STORAGE } },
	};

	private int permissionsRequestType;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		permissionsRequestType = getIntent().getIntExtra(X_PERMISSIONS_TYPE, 0);

		setContentView(R.layout.prompt_for_permissions);

		int promptTextId = (int) PERMISSIONS_REQUESTS[permissionsRequestType][0];
		setText(this, R.id.txtPermissionsPrompt, promptTextId, getAppName(this));
	}

	public void triggerPermissionRequest(View v) {
		ActivityCompat.requestPermissions(this, getPermissions(permissionsRequestType), 0);
	}

	@SuppressWarnings("PMD.UseVarargs")
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		// Whatever permissions the user granted (or failed to), we will carry on regardless.
		// If some permissions are lacking, the warning will be displayed again next time the app is started.
		int next = permissionsRequestType + 1;
		startPermissionsRequestIfRequired(this, next);
	}

	static void startPermissionsRequestIfRequired(Activity a) {
		startPermissionsRequestIfRequired(a, 0);
	}

	private static void startPermissionsRequestIfRequired(Activity a, int permissionsRequestType) {
		boolean shouldRequest = permissionsRequestType < PERMISSIONS_REQUESTS.length &&
				hasRequiredPermissions(a, permissionsRequestType);

		if(shouldRequest) {
			Intent i = new Intent(a, PromptForPermissionsActivity.class);
			i.putExtra(X_PERMISSIONS_TYPE, permissionsRequestType);
			a.startActivity(i);
		} else {
			startSettingsOrMainActivity(a);
		}
		a.finish();
	}

	private static boolean hasRequiredPermissions(Activity a, int permissionsRequestType) {
		for(String p : getPermissions(permissionsRequestType))
			if(ContextCompat.checkSelfPermission(a, p) != PERMISSION_GRANTED)
				return false;
		return true;
	}

	private static String[] getPermissions(int permissionsRequestType) {
		return (String[]) PERMISSIONS_REQUESTS[permissionsRequestType][1];
	}
}
