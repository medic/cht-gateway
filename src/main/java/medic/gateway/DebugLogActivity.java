package medic.gateway;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;

import static android.util.Log.*;

public class DebugLogActivity extends Activity {
	private static final String[] DEBUG_LOG_LIST_FROM = {
		"message",
		"date",
	};
	private static final int[] DEBUG_LOG_LIST_TO = {
		R.id.txtDebugLogMessage,
		R.id.txtDebugLogDate,
	};

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
		list.setAdapter(new SimpleAdapter(this,
				db.getLogEntries(200),
				R.layout.debug_log_item,
				DEBUG_LOG_LIST_FROM,
				DEBUG_LOG_LIST_TO));
	}
}
