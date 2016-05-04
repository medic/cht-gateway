package medic.gateway;

import android.app.*;
import android.content.*;
import android.webkit.*;
import android.os.*;

import static medic.gateway.Capabilities.*;
import static medic.gateway.GatewayLog.*;
import static medic.gateway.Utils.*;

public class StartupActivity extends Activity {
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		log("Starting...");

		if(SettingsStore.in(this).hasSettings()) {
			log("Starting MessageListsActivity...");
			startActivity(new Intent(this, MessageListsActivity.class));
		} else {
			log("Starting settings activity...");
			startSettingsActivity(this, getCapabilities());
		}

		finish();
	}

	private void log(String message, Object...extras) {
		trace(this, message, extras);
	}
}
