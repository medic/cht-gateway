package medic.gateway.alert;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TabHost;

import medic.android.ActivityBackgroundTask;

import static medic.gateway.alert.Capabilities.getCapabilities;
import static medic.gateway.alert.GatewayLog.logException;
import static medic.gateway.alert.GatewayLog.trace;
import static medic.gateway.alert.Utils.getAppName;
import static medic.gateway.alert.Utils.getAppVersion;
import static medic.gateway.alert.Utils.includeVersionNameInActivityTitle;
import static medic.gateway.alert.Utils.startSettingsActivity;
import static medic.gateway.alert.Utils.toast;

@SuppressWarnings("deprecation")
public class MessageListsActivity extends TabActivity {
	private static final long FIVE_MINUTES = 300000;

	private static final Class[] TAB_CLASSES = {
		GatewayEventLogActivity.class, WoListActivity.class, WtListActivity.class,
	};

	private Thinking thinking;

//> CLICK LISTENERS
	private final DialogInterface.OnClickListener deleteOldDataHandler = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
			thinking = Thinking.show(MessageListsActivity.this, R.string.txtDeleteOldData_inProgress);

			new DeleteTask(MessageListsActivity.this).execute();
		}
	};

	private final DialogInterface.OnClickListener cancelDialogHandler = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
			dialog.cancel();
		}
	};

	private final BroadcastReceiver pollUpdateReceiver = new BroadcastReceiver() {
		@Override public void onReceive(Context ctx, Intent i) {
			if(LastPoll.isStatusUpdate(i)) {
				updateForPollStatus();
			}
		}
	};

//> EVENT HANDLERS
	@Override protected void onCreate(Bundle savedInstanceState) {
		log("Starting...");
		super.onCreate(savedInstanceState);

		includeVersionNameInActivityTitle(this);

		TabHost tabHost = getTabHost();

		String[] tabs = getResources().getStringArray(R.array.message_lists_tabs);
		for(int i=0; i<tabs.length; ++i) {
			TabHost.TabSpec spec = tabHost.newTabSpec(tabs[i]);
			spec.setIndicator(tabs[i]);
			spec.setContent(new Intent(this, TAB_CLASSES[i]));
			tabHost.addTab(spec);
		}

		updateForPollStatus();

		LastPoll.register(this, pollUpdateReceiver);
	}

	@Override protected void onDestroy() {
		LastPoll.unregister(this, pollUpdateReceiver);
		if(thinking != null) thinking.dismiss();
		super.onDestroy();
	}

	@Override public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.message_list_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@SuppressLint("NonConstantResourceId")
	@Override public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.mnuMessageStats:
				thinking = MessageStatsDialog.show(this);
				return true;
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

//> PRIVATE HELPERS
	private void openSettings() {
		startSettingsActivity(this, getCapabilities());
		finish();
	}

	private void updateForPollStatus() {
		boolean pollingEnabled = SettingsStore.in(this).hasSettings() &&
				Settings.in(this).pollingEnabled;
		LastPoll last = LastPoll.getFrom(this);

		char c;
		Drawable icon;

		if(pollingEnabled && last != null) {
			if(last.wasSuccessful && last.timestamp + FIVE_MINUTES > System.currentTimeMillis()) {
				icon = baseIcon();
				c = '+';
			} else {
				icon = redIcon();
				c = '!';
			}
		} else {
			icon = grayscaleIcon();
			c = '-';
		}

		this.getActionBar().setIcon(icon);
		setTitle(String.format("%s %s v%s", c, getAppName(this), getAppVersion(this)));
	}

	private Drawable baseIcon() {
		return getResources().getDrawable(R.mipmap.icn_launcher).mutate();
	}

	private Drawable redIcon() {
		Drawable icon = baseIcon();

		icon.setColorFilter(0xffff0000, PorterDuff.Mode.MULTIPLY);

		return icon;
	}

	private Drawable grayscaleIcon() {
		Drawable icon = baseIcon();

		ColorMatrix matrix = new ColorMatrix();
		matrix.setSaturation(0);
		ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);

		icon.setColorFilter(filter);

		return icon;
	}

	private void log(String message, Object...extras) {
		trace(this, message, extras);
	}

	private static class DeleteTask extends ActivityBackgroundTask<MessageListsActivity, String, Void, Integer> {
		DeleteTask(MessageListsActivity a) {
			super(a);
		}

		protected Integer doInBackground(String... s) {
			try {
				MessageListsActivity ctx = getRequiredCtx("DeleteTask.doInBackground()");
				return Db.getInstance(ctx).deleteOldData();
			} catch(RuntimeException ex) {
				logException(ex, "Something went wrong deleting old data.");
				return -1;
			}
		}
		protected void onPostExecute(Integer deleteCount) {
			MessageListsActivity ctx = getRequiredCtx("MessageListsActivity.onPostExecute()");
			String message = ctx.getResources().getQuantityString(R.plurals.txtOldDataDeleteCount, deleteCount);
			toast(ctx, message, deleteCount);
			ctx.thinking.dismiss();
			ctx.recreate();
		}
	}
}
