package medic.gateway.alert;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.*;
import android.os.*;
import android.view.*;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;

import java.util.LinkedList;

import static medic.gateway.alert.Utils.*;
import static medic.gateway.alert.WoMessage.Status.*;

public class WoListActivity extends Activity {
	private static final int MAX_WO_MESSAGES = 100;

	private Db db;
	private ListView list;

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

		refreshList();
	}

	private void refreshList() {
		list.setAdapter(new WoMessageCursorAdapter(this,
				db.getWoMessages(MAX_WO_MESSAGES)));
	}
}

class WoMessageCursorAdapter extends ResourceCursorAdapter {
	private static final int NO_FLAGS = 0;

	public WoMessageCursorAdapter(Context ctx, Cursor c) {
		super(ctx, R.layout.wo_list_item, c, NO_FLAGS);
	}

	public void bindView(View v, Context ctx, Cursor c) {
		WoMessage m = Db.woMessageFrom(c);

		setText(v, R.id.txtWoStatus, m.status.toString());
		setText(v, R.id.txtWoLastAction, relativeTimestamp(m.lastAction));
		setText(v, R.id.txtWoTo, m.to);
		setText(v, R.id.txtWoContent, m.content);
	}
}

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
					Db db = Db.getInstance(activity);
					db.updateStatus(m, UNSENT);

					WoMessage updated = db.getWoMessage(m.id);

					View v = list.getChildAt(position);
					setText(v, R.id.txtWoStatus, updated.status.toString());
					setText(v, R.id.txtWoLastAction, relativeTimestamp(updated.lastAction));
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
