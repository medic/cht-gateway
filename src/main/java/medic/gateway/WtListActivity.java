package medic.gateway;

import android.app.*;
import android.os.*;

import static medic.gateway.BuildConfig.DEBUG;

public class WtListActivity extends Activity {
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.message_list_wt);
	}

	private void log(String message, Object...extras) {
		if(DEBUG) System.err.println("LOG | WtListActivity :: " +
				String.format(message, extras));
	}
}
