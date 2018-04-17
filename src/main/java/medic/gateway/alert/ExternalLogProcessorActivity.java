package medic.gateway.alert;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

import static medic.gateway.alert.GatewayLog.trace;
import static medic.gateway.alert.Utils.startSettingsOrMainActivity;

public class ExternalLogProcessorActivity extends Activity {
	private Thinking thinking;

//> LIFECYCLE
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		trace(this, "Starting...");

		boolean shouldProcess = ExternalLog.getInstance(this).shouldProcess();
		trace(this, "shouldProcess? %s", shouldProcess);

		if(shouldProcess) {
			setContentView(R.layout.external_log_processor);

			processExternalLog();
		} else startSettingsOrMainActivity(this);

		finish();
	}

	@Override public void onDestroy() {
		if(thinking != null) thinking.dismiss();
		super.onDestroy();
	}

//> PRIVATE HELPERS
	private void processExternalLog() {
		thinking = Thinking.show(this, R.string.txtProcessingExternalLog);
		AsyncTask.execute(new Runnable() {
			public void run() {
				Context ctx = ExternalLogProcessorActivity.this;
				ExternalLog.getInstance(ctx).process(ctx);
				thinking.dismiss();
				startSettingsOrMainActivity(ctx);
			}
		});
	}
}
