package medic.gateway.alert;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.AsyncTask;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TabHost;

import static medic.gateway.alert.Capabilities.getCapabilities;
import static medic.gateway.alert.GatewayLog.trace;
import static medic.gateway.alert.Utils.showSpinner;
import static medic.gateway.alert.Utils.startSettingsActivity;
import static medic.gateway.alert.Utils.toast;

@SuppressWarnings("deprecation")
public class MessageListsActivity extends TabActivity {
	private static final Class[] TAB_CLASSES = {
		GatewayEventLogActivity.class, WoListActivity.class, WtListActivity.class,
	};

	private final DialogInterface.OnClickListener deleteOldDataHandler = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
			final ProgressDialog spinner = showSpinner(MessageListsActivity.this, R.string.txtDeleteOldData_inProgress);

			new AsyncTask<String, Void, Integer>() {
				private final Context ctx = MessageListsActivity.this;

				protected Integer doInBackground(String..._) {
					try {
						return Db.getInstance(ctx).deleteOldData();
					} catch(RuntimeException ex) {
						return -1;
					}
				}
				protected void onPostExecute(Integer deleteCount) {
					String message = getResources().getQuantityString(R.plurals.txtOldDataDeleteCount, deleteCount);
					toast(ctx, message, deleteCount);
					spinner.dismiss();
					MessageListsActivity.this.recreate();
				}
			}.execute();
		}
	};

	private final DialogInterface.OnClickListener cancelDialogHandler = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
			dialog.cancel();
		}
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
			case R.id.mnuCompose:
				Intent composer;
				if(getCapabilities().isDefaultSmsProvider(this)) {
					composer = new Intent(this, ComposeSmsActivity.class);
				} else {
					composer = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:"));
				}
				startActivity(composer);
				return true;
			case R.id.mnuDeleteOldData:
				new AlertDialog.Builder(this)
					.setTitle(R.string.txtConfirmDeleteOldData_title)
					.setMessage(R.string.txtConfirmDeleteOldData_body)
					.setPositiveButton(R.string.btnConfirmDeleteOldData_confirm, deleteOldDataHandler)
					.setNegativeButton(R.string.btnCancel, cancelDialogHandler)
					.show();
				return true;
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
