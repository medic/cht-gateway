package medic.gateway;

import android.app.*;
import android.content.*;
import android.webkit.*;
import android.os.*;

import static medic.gateway.GatewayLog.*;

public class StartupActivity extends Activity {
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		log("Starting...");

		Class newActivity;
		if(SettingsStore.in(this).hasSettings()) {
			newActivity = MessageListsActivity.class;
		} else {
			newActivity = SettingsDialogActivity.class;
		}

		log("Starting new activity with class %s", newActivity);
		startActivity(new Intent(this, newActivity));

		finish();
	}

	private void log(String message, Object...extras) {
		trace(this, message, extras);
	}
}
