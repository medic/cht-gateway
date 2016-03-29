package medic.gateway;

import android.app.*;
import android.content.*;
import android.os.*;
import android.widget.*;

import static medic.gateway.BuildConfig.DEBUG;

public class MessageListsActivity extends TabActivity {
	private static final Class[] TAB_CLASSES = {
		WoListActivity.class, WtListActivity.class,
	};

	public void onCreate(Bundle savedInstanceState) {
		if(DEBUG) log("Starting...");
		super.onCreate(savedInstanceState);

		TabHost tabHost = getTabHost();

		String[] tabs = getResources().getStringArray(R.array.message_lists_tabs);
		for(int i=0; i<tabs.length; ++i) {
			TabHost.TabSpec spec = tabHost.newTabSpec(tabs[i]);
			spec.setIndicator(tabs[i]);
			spec.setContent(new Intent(this, TAB_CLASSES[i]));
			tabHost.addTab(spec);
		}
	}

	private void log(String message, Object...extras) {
		if(DEBUG) System.err.println("LOG | MessageListsActivity :: " +
				String.format(message, extras));
	}
}
