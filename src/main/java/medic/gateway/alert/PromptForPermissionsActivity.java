package medic.gateway.alert;

import android.Manifest.permission;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.view.View;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;
import static medic.gateway.alert.BuildConfig.IS_MEDIC_FLAVOUR;
import static medic.gateway.alert.GatewayLog.trace;
import static medic.gateway.alert.Utils.getAppName;
import static medic.gateway.alert.Utils.setText;

/**
 * To support Android 6.0+ (marshmallow), we must request SMS permissions at
 * runtime as well as in {@code AndroidManifest.xml}.
 * @see https://developer.android.com/intl/ru/about/versions/marshmallow/android-6.0-changes.html#behavior-runtime-permissions
 */
public class PromptForPermissionsActivity extends Activity implements ActivityCompat.OnRequestPermissionsResultCallback {
	private static final boolean REFUSE_TO_FUNCTION_WITHOUT_PERMISSIONS = IS_MEDIC_FLAVOUR;

	private static final String X_IS_DEMAND = "isDemand";
	private static final String X_PERMISSIONS_TYPE = "permissionsType";

	private static final Object[][] PERMISSIONS_REQUESTS = {
		/* sms */ { R.string.txtPermissionsPrompt_sms, new String[] { permission.SEND_SMS, permission.RECEIVE_SMS, permission.READ_PHONE_STATE } },
		/* file access */ { R.string.txtPermissionsPrompt_fileAccess, new String[] { permission.WRITE_EXTERNAL_STORAGE } },
	};

	private boolean isDemand;
	private boolean deniedBefore;
	private int permissionsRequestType;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		isDemand = getIntent().getBooleanExtra(X_IS_DEMAND, false);

		trace(this, "onCreate() :: isDemand=%s, permissionsRequestType=%s", isDemand, permissionsRequestType);

		setContentView(R.layout.prompt_for_permissions);

		int promptTextId;
		if(isDemand) {
			promptTextId = R.string.txtDemandPermissions;
		} else {
			permissionsRequestType = getIntent().getIntExtra(X_PERMISSIONS_TYPE, 0);
			promptTextId = (int) PERMISSIONS_REQUESTS[permissionsRequestType][0];
			makePermissionRequest();
		}
		setText(this, R.id.txtPermissionsPrompt, promptTextId, getAppName(this));
	}

	public void btnOk_onClick(View v) {
		if(isDemand) {
			// open app manager for this app
			Intent i = new Intent(ACTION_APPLICATION_DETAILS_SETTINGS,
					Uri.fromParts("package", getPackageName(), null));
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(i);

			finish();
		} else makePermissionRequest();
	}

	@SuppressWarnings("PMD.UseVarargs")
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		boolean allGranted = true;
		for(int res : grantResults) allGranted &= res == PERMISSION_GRANTED;

		if(allGranted) {
			nextActivity(this, permissionsRequestType + 1);
		} else if(REFUSE_TO_FUNCTION_WITHOUT_PERMISSIONS) {
			// For some flavours, we don't want to give people the option to use the app without the
			// correct permissions.  If the permission is not granted, re-request the same.
			if(canShowPromptFor(this, permissionsRequestType)) { // NOPMD
				// Don't do anything - the user can re-read the on-screen advice.
			} else {
				// The user has checked the "don't ask me again"/"never allow" box (TODO which one?), so we have to step things up.
				startActivity(demandPermissions(this));
				finish();
			}
		} else {
			if(!deniedBefore && canShowPromptFor(this, permissionsRequestType)) {
				// Allow user to read the advice on the screen
				deniedBefore = true;
			} else nextActivity(this, permissionsRequestType + 1);
		}
	}

//> PRIVATE HELPERS
	private void makePermissionRequest() {
		ActivityCompat.requestPermissions(this, getPermissions(permissionsRequestType), 0);
	}

//> STATIC UTILS
	static void startPermissionsRequestChain(Activity a) {
		nextActivity(a, 0);
	}

//> STATIC HELPERS
	private static void nextActivity(Activity a, int firstPermissionToConsider) {
		trace(a, "nextActivity() :: %s", firstPermissionToConsider);

		Intent next = null;

		for(int p=firstPermissionToConsider; p<PERMISSIONS_REQUESTS.length; ++p) {
			if(!hasRequiredPermissions(a, p)) {
				next = requestPermission(a, p);
				break;
			}
		}

		if(next == null) next = new Intent(a, ExternalLogProcessorActivity.class);

		trace(a, "nextActivity() :: Should start activity with intent: %s", next);

		a.startActivity(next);
		a.finish();
	}

	private static Intent requestPermission(Activity a, int permissionsRequestType) {
		trace(a, "requestPermission() :: p=%s", permissionsRequestType);
		Intent i = new Intent(a, PromptForPermissionsActivity.class);
		i.putExtra(X_PERMISSIONS_TYPE, permissionsRequestType);
		i.putExtra(X_IS_DEMAND, false);
		return i;
	}

	private static Intent demandPermissions(Activity a) {
		trace(a, "demandPermission()");
		Intent i = new Intent(a, PromptForPermissionsActivity.class);
		i.putExtra(X_IS_DEMAND, true);
		return i;
	}

	private static boolean canShowPromptFor(Activity a, int permissionsRequestType) {
		trace(a, "canShowPromptFor() p=%s", permissionsRequestType);
		for(String p : getPermissions(permissionsRequestType)) {
			boolean shouldShow = ActivityCompat.shouldShowRequestPermissionRationale(a, p);
			trace(a, "canShowPromptFor() can %s? %s", p, shouldShow);
			if(!shouldShow) return false;
		}
		return true;
	}

	private static boolean hasRequiredPermissions(Activity a, int permissionsRequestType) {
		trace(a, "hasRequiredPermissions() :: %s", permissionsRequestType);
		for(String p : getPermissions(permissionsRequestType))
			if(ContextCompat.checkSelfPermission(a, p) != PERMISSION_GRANTED)
				return false;
		return true;
	}

	private static String[] getPermissions(int permissionsRequestType) {
		return (String[]) PERMISSIONS_REQUESTS[permissionsRequestType][1];
	}
}
