package medic.gateway.alert;

import android.app.*;
import android.content.*;
import android.webkit.*;
import android.os.*;

import static medic.gateway.alert.GatewayLog.*;
import static medic.gateway.alert.Utils.*;

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
