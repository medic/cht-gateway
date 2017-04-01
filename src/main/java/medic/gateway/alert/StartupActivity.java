package medic.gateway.alert;

import android.app.Activity;
import android.os.Bundle;

import static medic.gateway.alert.GatewayLog.trace;
import static medic.gateway.alert.PromptForPermissionsActivity.startPermissionsRequestChain;

public class StartupActivity extends Activity {
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		trace(this, "Starting...");

		startPermissionsRequestChain(this);

		finish();
	}
}
