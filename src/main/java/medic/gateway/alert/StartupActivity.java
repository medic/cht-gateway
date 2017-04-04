package medic.gateway.alert;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import static medic.gateway.alert.GatewayLog.trace;
import static medic.gateway.alert.Utils.hasRequiredPermissions;
import static medic.gateway.alert.Utils.startSettingsOrMainActivity;

public class StartupActivity extends Activity {
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		log("Starting...");

		if(!hasRequiredPermissions(this)) {
			startActivity(new Intent(this, PromptForSmsPermissionsActivity.class));
		} else startSettingsOrMainActivity(this);

		finish();
	}

	private void log(String message, Object...extras) {
		trace(this, message, extras);
	}
}
