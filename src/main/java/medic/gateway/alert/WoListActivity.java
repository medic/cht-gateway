package medic.gateway.alert;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;

import java.util.LinkedList;

import static medic.gateway.alert.GatewayLog.*;
import static medic.gateway.alert.Utils.*;
import static medic.gateway.alert.WoMessage.Status.*;

public class WoListActivity extends Activity {
	private static final int MAX_WO_MESSAGES = 100;

	private Db db;
	private ListView list;
	private SparseArray<String> checklist;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.message_list_wo);

		db = Db.getInstance(this);

		list = (ListView) findViewById(R.id.lstWoMessages);
		list.setOnItemClickListener(new WoListItemClickListener(this, list));

		((Button) findViewById(R.id.btnRefreshWoMessageList))
				.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) { refreshList(); }
		});

		((Button) findViewById(R.id.btnRetrySelected))
				.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) { retrySelected(); }
		});

		((Button) findViewById(R.id.btnSelectNewer))
				.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) { selectNewer(); }
		});

		refreshList();
	}

	void retry(String id, int position) {
		trace(this, "Retrying message at %d with id %s...", position, id);

		WoMessage m = db.getWoMessage(id);

		if(m.status.canBeRetried()) {
			db.updateStatus(m, UNSENT);

			WoMessage updated = db.getWoMessage(id);

			View v = list.getChildAt(position);
			setText(v, R.id.txtWoStatus, updated.status.toString());
			setText(v, R.id.txtWoLastAction, relativeTimestamp(updated.lastAction));
		}
	}

	void updateChecked(String id, int position, boolean isChecked) {
		if(isChecked) checklist.put(position, id);
		else checklist.delete(position);

		findViewById(R.id.btnRetrySelected).setEnabled(checklist.size() > 0);
		findViewById(R.id.btnSelectNewer).setEnabled(checklist.size() > 0);
	}

	private void refreshList() {
		checklist = new SparseArray<String>();
		list.setAdapter(new WoMessageCursorAdapter(this,
				db.getWoMessages(MAX_WO_MESSAGES)));

		findViewById(R.id.btnRetrySelected).setEnabled(false);
		findViewById(R.id.btnSelectNewer).setEnabled(false);
	}

	private void retrySelected() {
		for(int i=checklist.size()-1; i>=0; --i) {
			retry(checklist.valueAt(i), checklist.keyAt(i));
		}
	}

	private void selectNewer() {
		int lastSelectedIndex = checklist.keyAt(checklist.size() - 1);
		for(int i=lastSelectedIndex-1; i>=0; --i) {
			((CheckBox) list.getChildAt(i).findViewById(R.id.cbxMessage)).setChecked(true);
		}
	}
}

// TODO should this be an inner class?
class WoMessageCursorAdapter extends ResourceCursorAdapter {
	private static final int NO_FLAGS = 0;

	private final WoListActivity activity;

	public WoMessageCursorAdapter(WoListActivity activity, Cursor c) {
		super(activity, R.layout.wo_list_item, c, NO_FLAGS);
		this.activity = activity;
	}

	public void bindView(View v, Context ctx, Cursor c) {
		final WoMessage m = Db.woMessageFrom(c);

		setText(v, R.id.txtWoStatus, m.status.toString());
		setText(v, R.id.txtWoLastAction, relativeTimestamp(m.lastAction));
		setText(v, R.id.txtWoTo, m.to);
		setText(v, R.id.txtWoContent, m.content);

		CheckBox cbx = (CheckBox) v.findViewById(R.id.cbxMessage);
		cbx.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton btn, boolean isChecked) {
				View listItem = (View) btn.getParent();
				int listIndex = ((ViewGroup) listItem.getParent()).indexOfChild(listItem);

				trace(this, "Changed checkbox at %d to %s", listIndex, isChecked);

				activity.updateChecked(m.id, listIndex, isChecked);
			}
		});
	}
}

// TODO should this be an inner class?
class WoListItemClickListener implements AdapterView.OnItemClickListener {
	private static final DialogInterface.OnClickListener NO_CLICK_LISTENER = null;

	private final WoListActivity activity;
	private final ListView list;

	WoListItemClickListener(WoListActivity activity, ListView list) {
		this.activity = activity;
		this.list = list;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
		Cursor c = (Cursor) list.getItemAtPosition(position);

		WoMessage m = Db.getInstance(activity).getWoMessage(c.getString(0));

		messageDetailDialog(m, position).show();
	}

	private AlertDialog messageDetailDialog(final WoMessage m, final int position) {
		LinkedList<String> content = new LinkedList<>();

		content.add(string(R.string.lblTo, m.to));
		if(m.status == FAILED) {
			content.add(string(R.string.lblStatusWithCause, m.status, m.getFailureReason()));
		} else {
			content.add(string(R.string.lblStatus, m.status));
		}
		content.add(string(R.string.lblLastAction, relativeTimestamp(m.lastAction)));

		content.add(string(R.string.lblContent, m.content));

		AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
		if(m.status.canBeRetried()) {
			dialog.setPositiveButton(R.string.btnRetry, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					activity.retry(m.id, position);
				}
			});
		}

		dialog.setItems(content.toArray(new String[content.size()]), NO_CLICK_LISTENER);

		return dialog.create();
	}

	private final String string(int stringId, Object...args) {
		return String.format(activity.getString(stringId), args);
	}
}
