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

import medic.gateway.alert.WtMessage.Status;

import static medic.gateway.alert.GatewayLog.*;
import static medic.gateway.alert.Utils.*;
import static medic.gateway.alert.WtMessage.Status.*;

public class WtListActivity extends Activity {
	private static final int MAX_WT_MESSAGES = 100;

	private Db db;
	private ListView list;
	private SparseArray<String> checklist;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.message_list_wt);

		db = Db.getInstance(this);

		list = (ListView) findViewById(R.id.lstWtMessages);
		list.setOnItemClickListener(new WtListItemClickListener(this, list));

		((Button) findViewById(R.id.btnRefreshWtMessageList))
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

		WtMessage m = db.getWtMessage(id);

		if(m.getStatus().canBeRetried()) {
			Status oldStatus = m.getStatus();
			m.setStatus(WAITING);
			db.updateStatusFrom(oldStatus, m);

			WtMessage updated = db.getWtMessage(id);

			View v = list.getChildAt(position);
			setText(v, R.id.txtWtStatus, updated.getStatus().toString());
			setText(v, R.id.txtWtLastAction, relativeTimestamp(updated.getLastAction()));
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
		list.setAdapter(new WtMessageCursorAdapter(this,
				db.getWtMessages(MAX_WT_MESSAGES)));

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

// TODO should this be an inner class?  or a separate class with an interface for CheckableList?
class WtMessageCursorAdapter extends ResourceCursorAdapter {
	private static final int NO_FLAGS = 0;

	private final WtListActivity activity;

	public WtMessageCursorAdapter(WtListActivity activity, Cursor c) {
		super(activity, R.layout.wt_list_item, c, NO_FLAGS);
		this.activity = activity;
	}

	public void bindView(View v, Context ctx, Cursor c) {
		final WtMessage m = Db.wtMessageFrom(c);

		setText(v, R.id.txtWtStatus, m.getStatus().toString());
		setText(v, R.id.txtWtLastAction, relativeTimestamp(m.getLastAction()));
		setText(v, R.id.txtWtFrom, m.from);
		setText(v, R.id.txtWtContent, m.content);

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
class WtListItemClickListener implements AdapterView.OnItemClickListener {
	private static final DialogInterface.OnClickListener NO_CLICK_LISTENER = null;

	private final WtListActivity activity;
	private final ListView list;

	WtListItemClickListener(WtListActivity activity, ListView list) {
		this.activity = activity;
		this.list = list;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
		Cursor c = (Cursor) list.getItemAtPosition(position);

		// Get a fresh copy of the message, in case it's been updated
		// more recently than the list
		WtMessage m = Db.getInstance(activity).getWtMessage(c.getString(0));

		messageDetailDialog(m, position).show();
	}

	private AlertDialog messageDetailDialog(final WtMessage m, final int position) {
		LinkedList<String> content = new LinkedList<>();

		content.add(string(R.string.lblFrom, m.from));
		content.add(string(R.string.lblStatus, m.getStatus()));
		content.add(string(R.string.lblLastAction, relativeTimestamp(m.getLastAction())));
		content.add(string(R.string.lblContent, m.content));

		AlertDialog.Builder dialog = new AlertDialog.Builder(activity);

		if(m.getStatus().canBeRetried()) {
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
