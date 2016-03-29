package medic.gateway;

import android.app.*;
import android.os.*;

import static medic.gateway.BuildConfig.DEBUG;

public class WoListActivity extends Activity {
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.message_list_wo);
	}

	private void log(String message, Object...extras) {
		if(DEBUG) System.err.println("LOG | WoListActivity :: " +
				String.format(message, extras));
	}
}
