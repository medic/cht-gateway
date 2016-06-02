package medic.gateway.alert;

import android.app.*;
import android.content.*;
import android.view.*;
import android.os.*;
import android.widget.*;

import static medic.gateway.alert.Capabilities.*;
import static medic.gateway.alert.GatewayLog.*;
import static medic.gateway.alert.Utils.*;

@SuppressWarnings("deprecation")
public class MessageListsActivity extends TabActivity {
	private static final Class[] TAB_CLASSES = {
		GatewayEventLogActivity.class, WoListActivity.class, WtListActivity.class,
	};

	public void onCreate(Bundle savedInstanceState) {
		log("Starting...");
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

	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.message_list_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.mnuSettings:
				openSettings();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void openSettings() {
		startSettingsActivity(this, getCapabilities());
		finish();
	}

	private void log(String message, Object...extras) {
		trace(this, message, extras);
	}
}
