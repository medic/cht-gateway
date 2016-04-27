package medic.gateway;

import android.app.*;
import android.content.*;
import android.database.*;
import android.os.*;
import android.view.*;
import android.widget.*;

import java.text.*;
import java.util.*;

public class DebugLogActivity extends Activity {
	private static final int MAX_LOG_ITEMS = 200;

	private Db db;
	private ListView list;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.debug_log);

		db = Db.getInstance(this);
		list = (ListView) findViewById(R.id.lstDebugLog);

		((Button) findViewById(R.id.btnRefreshLog))
				.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) { refreshList(); }
		});

		refreshList();
	}

	private void refreshList() {
		list.setAdapter(new DebugLogEntryCursorAdapter(this, db.getLogEntries(MAX_LOG_ITEMS)));
	}
}

class DebugLogEntryCursorAdapter extends ResourceCursorAdapter {
	private static final int NO_FLAGS = 0;

	public DebugLogEntryCursorAdapter(Context ctx, Cursor c) {
		super(ctx, R.layout.debug_log_item, c, NO_FLAGS);
	}

	public void bindView(View v, Context ctx, Cursor c) {
		setText(v, R.id.txtDebugLogDate, formatDate(c.getLong(1)));
		setText(v, R.id.txtDebugLogMessage, c.getString(2));
	}

	private void setText(View v, int textViewId, String text) {
		TextView tv = (TextView) v.findViewById(textViewId);
		tv.setText(text);
	}

	private String formatDate(long timestamp) {
		return SimpleDateFormat.getDateTimeInstance()
				.format(new Date(timestamp));
	}
}
