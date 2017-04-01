package medic.gateway.alert;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

import static medic.gateway.alert.GatewayLog.trace;
import static medic.gateway.alert.PromptForPermissionsActivity.startPermissionsRequestIfRequired;
import static medic.gateway.alert.Utils.showSpinner;

public class StartupActivity extends Activity {
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		log("Starting...");

		if(ExternalLog.getInstance(this).shouldProcess()) processExternalLog();
		else nextActivity();
	}

	private void nextActivity() {
		startPermissionsRequestIfRequired(this);
	}

	private void processExternalLog() {
		final ProgressDialog spinner = showSpinner(this, R.string.txtProcessingExternalLog);
		AsyncTask.execute(new Runnable() {
			public void run() {
				Context ctx = StartupActivity.this;
				ExternalLog.getInstance(ctx).process(ctx);
				spinner.dismiss();
			}
		});
	}

	private void log(String message, Object...extras) {
		trace(this, message, extras);
	}
}
