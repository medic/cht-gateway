package medic.gateway.alert;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import androidx.fragment.app.FragmentActivity;

import medic.gateway.alert.WtMessage.Status;

import static medic.gateway.alert.GatewayLog.logException;
import static medic.gateway.alert.GatewayLog.trace;
import static medic.gateway.alert.Utils.absoluteTimestamp;
import static medic.gateway.alert.Utils.showAlert;
import static medic.gateway.alert.Utils.NO_CLICK_LISTENER;
import static medic.gateway.alert.WtMessage.Status.WAITING;

public class WtListActivity extends FragmentActivity {
	private Db db;
	private Set<String> checkedMessageIds;
	private Thinking thinking;

//> LIFECYCLE
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.message_list_wt);

		this.db = Db.getInstance(this);

		((Button) findViewById(R.id.btnRefreshWtMessageList))
				.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) { refreshList(); }
		});

		((Button) findViewById(R.id.btnRetrySelected))
				.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) { retrySelected(); }
		});

		refreshList();
	}

	@Override public void onDestroy() {
		if(thinking != null) thinking.dismiss();
		super.onDestroy();
	}

//> API FOR WtListFragment
	boolean isChecked(WtMessage m) {
		return checkedMessageIds.contains(m.id);
	}

	void updateChecked(WtMessage m, boolean isChecked) {
		if(isChecked) checkedMessageIds.add(m.id);
		else checkedMessageIds.remove(m.id);

		findViewById(R.id.btnRetrySelected).setEnabled(!checkedMessageIds.isEmpty());
	}

	void showMessageDetailDialog(final WtMessage m, final int position) {
		thinking = Thinking.show(this);
		AsyncTask.execute(new Runnable() {
			public void run() {
				try {
					LinkedList<String> content = new LinkedList<>();

					content.add(string(R.string.lblFrom, m.from));
					content.add(string(R.string.lblContent, m.content));
					content.add(string(R.string.lblSent, absoluteTimestamp(m.smsSent)));
					content.add(string(R.string.lblReceived, absoluteTimestamp(m.smsReceived)));
					content.add(string(R.string.lblStatusUpdates));

					List<WtMessage.StatusUpdate> updates = db.getStatusUpdates(m);
					Collections.reverse(updates);
					for(WtMessage.StatusUpdate u : updates) {
						content.add(String.format("%s: %s", absoluteTimestamp(u.timestamp), u.newStatus));
					}

					final AlertDialog.Builder dialog = new AlertDialog.Builder(WtListActivity.this);
					if(m.getStatus().canBeRetried()) {
						dialog.setPositiveButton(R.string.btnRetry, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								retry(m.id);
								resetScroll();
								refreshList();
							}
						});
					}

					dialog.setItems(content.toArray(new String[content.size()]), NO_CLICK_LISTENER);

					showAlert(WtListActivity.this, dialog);
				} catch(Exception ex) {
					logException(WtListActivity.this, ex, "Failed to load WT message details.");
				} finally {
					thinking.dismiss();
				}
			}
		});
	}

//> PRIVATE HELPERS
	private void retry(String id) {
		trace(this, "Retrying message with id %s...", id);

		WtMessage m = db.getWtMessage(id);

		if(!m.getStatus().canBeRetried()) return;

		Status oldStatus = m.getStatus();
		m.setStatus(WAITING);
		db.updateStatusFrom(oldStatus, m);
	}

	private final String string(int stringId, Object...args) {
		return getString(stringId, args);
	}

	private void resetScroll() {
		// This implementation is far from ideal, but at least it works.
		// Which is more than can be said for more logical options like:
		// - getFragment().setSelection(0);
		// - getFragment().getListView().setSelection(0);
		// - getFragment().getListView().setSelectionAfterHeaderView();
		// ...and doing all of the above inside an AsyncTask.
		getFragment().getListView().smoothScrollToPosition(0);
	}

	private void refreshList() {
		checkedMessageIds = new HashSet<String>();

		getSupportLoaderManager().restartLoader(WtListFragment.LOADER_ID, null, getFragment());

		findViewById(R.id.btnRetrySelected).setEnabled(false);
	}

	private void retrySelected() {
		resetScroll();

		for(String id : checkedMessageIds) retry(id);

		refreshList();
	}

	private WtListFragment getFragment() {
		return (WtListFragment) getSupportFragmentManager()
				.findFragmentById(R.id.lstWtMessages);
	}
}
