package medic.gateway;

import android.app.*;
import android.content.*;
import android.database.*;
import android.os.*;
import android.view.*;
import android.widget.*;

public class WoListActivity extends Activity {
	private static final int MAX_WO_MESSAGES = 100;

	private Db db;
	private ListView list;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.message_list_wo);

		db = Db.getInstance(this);
		list = (ListView) findViewById(R.id.lstWoMessages);

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
		String status = c.getString(1);
		long lastAction = c.getLong(2);
		String to = c.getString(3);
		String content = c.getString(4);

		setText(v, R.id.txtWoStatus, status);
		setText(v, R.id.txtWoLastAction, Utils.relativeTimestamp(lastAction));
		setText(v, R.id.txtWoTo, to);
		setText(v, R.id.txtWoContent, content);
	}

	private void setText(View v, int textViewId, String text) {
		TextView tv = (TextView) v.findViewById(textViewId);
		tv.setText(text);
	}
}
