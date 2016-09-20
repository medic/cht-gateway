package medic.gateway.alert;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

public class GatewayEventLogActivity extends FragmentActivity {
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.event_log);

		((Button) findViewById(R.id.btnRefreshLog))
				.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) { refreshList(); }
		});
	}

	private void refreshList() {
		Fragment genericFragment = getSupportFragmentManager().findFragmentById(R.id.lstGatewayEventLog);
		GatewayEventLogFragment fragment = (GatewayEventLogFragment) genericFragment;
		getSupportLoaderManager().restartLoader(GatewayEventLogFragment.LOADER_ID, null, fragment);
	}
}
