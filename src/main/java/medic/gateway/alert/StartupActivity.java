package medic.gateway.alert;

import android.app.Activity;
import android.os.Bundle;

import static medic.gateway.alert.GatewayLog.trace;
import static medic.gateway.alert.PromptForPermissionsActivity.startPermissionsRequestIfRequired;

public class StartupActivity extends Activity {
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		log("Starting...");

		startPermissionsRequestIfRequired(this);
	}

	private void log(String message, Object...extras) {
		trace(this, message, extras);
	}
}
