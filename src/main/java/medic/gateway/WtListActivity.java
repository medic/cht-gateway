package medic.gateway;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;

import static medic.gateway.BuildConfig.DEBUG;

public class WtListActivity extends Activity {
	private static final int MAX_WT_MESSAGES = 100;

	private static final String[] WT_LIST_FROM = {
		"from",
		"status",
		"content",
		"lastAction",
	};
	private static final int[] WT_LIST_TO = {
		R.id.txtWtFrom,
		R.id.txtWtStatus,
		R.id.txtWtContent,
		R.id.txtWtLastAction,
	};

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
		list.setAdapter(new SimpleAdapter(this,
				db.getWtMessages(MAX_WT_MESSAGES),
				R.layout.wt_list_item,
				WT_LIST_FROM,
				WT_LIST_TO));
	}

	private void log(String message, Object...extras) {
		if(DEBUG) System.err.println("LOG | WtListActivity :: " +
				String.format(message, extras));
	}
}
