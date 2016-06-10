package medic.gateway.alert;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;

import java.util.LinkedList;

import medic.gateway.alert.WtMessage.Status;

import static medic.gateway.alert.Utils.*;
import static medic.gateway.alert.WtMessage.Status.*;

public class WtListActivity extends Activity {
	private static final int MAX_WT_MESSAGES = 100;

	private Db db;
	private ListView list;

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

		refreshList();
	}

	private void refreshList() {
		list.setAdapter(new WtMessageCursorAdapter(this,
				db.getWtMessages(MAX_WT_MESSAGES)));
	}
}

class WtMessageCursorAdapter extends ResourceCursorAdapter {
	private static final int NO_FLAGS = 0;

	public WtMessageCursorAdapter(Context ctx, Cursor c) {
		super(ctx, R.layout.wt_list_item, c, NO_FLAGS);
	}

	public void bindView(View v, Context ctx, Cursor c) {
		WtMessage m = Db.wtMessageFrom(c);

		setText(v, R.id.txtWtStatus, m.getStatus().toString());
		setText(v, R.id.txtWtLastAction, relativeTimestamp(m.getLastAction()));
		setText(v, R.id.txtWtFrom, m.from);
		setText(v, R.id.txtWtContent, m.content);
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
		final WtMessage m = Db.wtMessageFrom(c);

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
					Status oldStatus = m.getStatus();
					m.setStatus(WAITING);
					Db.getInstance(activity).updateStatusFrom(oldStatus, m);

					View v = list.getChildAt(position);
					setText(v, R.id.txtWtStatus, m.getStatus().toString());
					setText(v, R.id.txtWtLastAction, relativeTimestamp(m.getLastAction()));
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
