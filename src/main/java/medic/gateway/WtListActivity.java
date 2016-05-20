package medic.gateway;

import android.app.*;
import android.content.Context;
import android.content.DialogInterface;
import android.database.*;
import android.os.*;
import android.view.*;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;

import medic.gateway.WtMessage.Status;

import static medic.gateway.Utils.*;
import static medic.gateway.WtMessage.Status.*;

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

class WtListItemClickListener implements AdapterView.OnItemClickListener {
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

		if(m.getStatus().canBeRetried()) {
			retryDialog(m, position).show();
		}
	}

	private AlertDialog retryDialog(final WtMessage m, final int position) {
		return new AlertDialog.Builder(activity)
				.setTitle(R.string.txtRetryTitle)
				.setMessage(String.format(activity.getString(R.string.txtRetryWtBody), m.from))
				.setPositiveButton(R.string.btnRetry, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Status oldStatus = m.getStatus();
						m.setStatus(WAITING);
						Db.getInstance(activity).updateStatusFrom(oldStatus, m);

						View v = list.getChildAt(position);
						setText(v, R.id.txtWtStatus, m.getStatus().toString());
						setText(v, R.id.txtWtLastAction, relativeTimestamp(m.getLastAction()));
					}
				})
				.create();
	}
}
