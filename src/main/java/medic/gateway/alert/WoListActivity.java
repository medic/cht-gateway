package medic.gateway.alert;

import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;

import android.support.v4.app.FragmentActivity;

import static medic.gateway.alert.GatewayLog.*;
import static medic.gateway.alert.Utils.*;
import static medic.gateway.alert.WoMessage.Status.*;

public class WoListActivity extends FragmentActivity {
	private SparseArray<String> checklist;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.message_list_wo);

		((Button) findViewById(R.id.btnRefreshWoMessageList))
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

	void updateChecked(String id, int position, boolean isChecked) {
		if(isChecked) checklist.put(position, id);
		else checklist.delete(position);

		findViewById(R.id.btnRetrySelected).setEnabled(checklist.size() > 0);
		findViewById(R.id.btnSelectNewer).setEnabled(checklist.size() > 0);
	}

	private void refreshList() {
		checklist = new SparseArray<String>();

		getSupportLoaderManager().restartLoader(WoListFragment.LOADER_ID, null, getFragment());

		findViewById(R.id.btnRetrySelected).setEnabled(false);
		findViewById(R.id.btnSelectNewer).setEnabled(false);
	}

	private void retrySelected() {
		for(int i=checklist.size()-1; i>=0; --i) {
			getFragment().retry(checklist.valueAt(i), checklist.keyAt(i));
		}
	}

	private void selectNewer() {
		int lastSelectedIndex = checklist.keyAt(checklist.size() - 1);
		for(int i=lastSelectedIndex-1; i>=0; --i) {
			((CheckBox) getList().getChildAt(i).findViewById(R.id.cbxMessage)).setChecked(true);
		}
	}

	private WoListFragment getFragment() {
		return (WoListFragment) getSupportFragmentManager()
				.findFragmentById(R.id.lstWoMessages);
	}

	private ListView getList() {
		return getFragment().getListView();
	}
}
