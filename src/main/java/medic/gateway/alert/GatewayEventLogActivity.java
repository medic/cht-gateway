package medic.gateway.alert;

import android.app.*;
import android.content.*;
import android.database.*;
import android.os.*;
import android.view.*;
import android.widget.*;

import java.text.*;
import java.util.*;

import static medic.gateway.alert.Utils.*;

public class GatewayEventLogActivity extends Activity {
	private static final int MAX_LOG_ITEMS = 200;
	private static final boolean DEFER_LOADING = true;

	private Db db;
	private ListView list;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.event_log);

		db = Db.getInstance(this);
		list = (ListView) findViewById(R.id.lstGatewayEventLog);

		((Button) findViewById(R.id.btnRefreshLog))
				.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) { refreshList(); }
		});

		if(DEFER_LOADING) {
			// Don't refresh list on first load - instead add a list
			// item explaining that the list must be refreshed.
			// TODO we *should* refresh list on first load, but only
			// once doing this does not impact performance.
			list.setAdapter(new ArrayAdapter<String>(this,
					R.layout.event_log_empty_item,
					new String[]{ getString(R.string.txtEmptyListPrompt) }));
		} else {
			refreshList();
		}
	}

	private void refreshList() {
		list.setAdapter(new GatewayEventLogEntryCursorAdapter(this, db.getLogEntries(MAX_LOG_ITEMS)));
	}
}

class GatewayEventLogEntryCursorAdapter extends ResourceCursorAdapter {
	private static final int NO_FLAGS = 0;

	public GatewayEventLogEntryCursorAdapter(Context ctx, Cursor c) {
		super(ctx, R.layout.event_log_item, c, NO_FLAGS);
	}

	public void bindView(View v, Context ctx, Cursor c) {
		setText(v, R.id.txtGatewayEventLogDate, formatDate(c.getLong(1)));
		setText(v, R.id.txtGatewayEventLogMessage, c.getString(2));
	}

	private String formatDate(long timestamp) {
		return SimpleDateFormat.getDateTimeInstance()
				.format(new Date(timestamp));
	}
}
