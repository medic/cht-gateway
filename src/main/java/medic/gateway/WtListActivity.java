package medic.gateway;

import android.app.*;
import android.content.*;
import android.database.*;
import android.os.*;
import android.view.*;
import android.widget.*;

public class WtListActivity extends Activity {
	private static final int MAX_WT_MESSAGES = 100;

	private Db db;
	private ListView list;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.message_list_wt);

		db = Db.getInstance(this);
		list = (ListView) findViewById(R.id.lstWtMessages);

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
		String status = c.getString(1);
		long lastAction = c.getLong(2);
		String from = c.getString(3);
		String content = c.getString(4);

		setText(v, R.id.txtWtStatus, status);
		setText(v, R.id.txtWtLastAction, Utils.relativeTimestamp(lastAction));
		setText(v, R.id.txtWtFrom, from);
		setText(v, R.id.txtWtContent, content);
	}

	private void setText(View v, int textViewId, String text) {
		TextView tv = (TextView) v.findViewById(textViewId);
		tv.setText(text);
	}
}
