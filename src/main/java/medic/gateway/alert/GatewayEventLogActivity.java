package medic.gateway.alert;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

public class GatewayEventLogActivity extends FragmentActivity {
	@Override protected void onCreate(Bundle savedInstanceState) {
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
