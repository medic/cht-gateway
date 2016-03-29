package medic.gateway;

import android.app.*;
import android.content.*;
import android.webkit.*;
import android.os.*;

public class StartupActivity extends Activity {
	private static final boolean DEBUG = BuildConfig.DEBUG;

	public void onCreate(Bundle savedInstanceState) {
		if(DEBUG) log("Starting...");
		super.onCreate(savedInstanceState);

		Class newActivity;
		if(SettingsStore.in(this).hasSettings()) {
			newActivity = MessageListsActivity.class;
		} else {
			newActivity = SettingsDialogActivity.class;
		}

		if(DEBUG) log("Starting new activity with class %s", newActivity);
		startActivity(new Intent(this, newActivity));

		finish();
	}

	private void log(String message, Object...extras) {
		if(DEBUG) System.err.println("LOG | StartupActivity :: " +
				String.format(message, extras));
	}
}
