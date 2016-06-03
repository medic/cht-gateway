package medic.gateway.alert;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import static medic.gateway.alert.BuildConfig.DEBUG;
import static medic.gateway.alert.GatewayLog.logEvent;
import static medic.gateway.alert.GatewayLog.logException;
import static medic.gateway.alert.GatewayLog.trace;

public class SettingsDialogActivity extends Activity {
	private boolean hasPreviousSettings;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		log("Starting...");

		SettingsStore store = SettingsStore.in(this);
		hasPreviousSettings = store.hasSettings();

		setContentView(R.layout.settings_dialog);

		if(hasPreviousSettings) {
			Settings settings = store.get();

			text(R.id.txtWebappUrl, settings.getWebappUrl());
			check(R.id.cbxEnablePolling, settings.isPollingEnabled());
		} else {
			cancelButton().setVisibility(View.GONE);
		}
	}

//> EVENT HANDLERS
	public void verifyAndSave(View view) {
		log("verifyAndSave");

		submitButton().setEnabled(false);
		cancelButton().setEnabled(false);

		String webappUrl = text(R.id.txtWebappUrl);
		final boolean syncEnabled = checked(R.id.cbxEnablePolling);

		final ProgressDialog spinner = showSpinner(String.format(
				getString(R.string.txtValidatingWebappUrl),
				webappUrl));

		new AsyncTask<String, Void, WebappUrlVerififcation>() {
			protected WebappUrlVerififcation doInBackground(String... webappUrl) {
				if(DEBUG && webappUrl.length != 1) throw new AssertionError();
				return new WebappUrlVerifier().verify(webappUrl[0]);
			}
			protected void onPostExecute(WebappUrlVerififcation result) {
				if(result.isOk) {
					boolean savedOk = saveSettings(new Settings(result.webappUrl, syncEnabled));
					if(savedOk) {
						logEvent(SettingsDialogActivity.this, "Settings saved.  Webapp URL: %s", result.webappUrl);
						startApp();
					}
				} else {
					showError(R.id.txtWebappUrl, result.failure);
					submitButton().setEnabled(true);
					cancelButton().setEnabled(true);
				}
				spinner.dismiss();
			}
		}.execute(webappUrl);
	}

	public void cancelSettingsEdit(View view) {
		log("cancelSettingsEdit");
		backToMessageListsView();
	}

	public void onBackPressed() {
		if(hasPreviousSettings) {
			backToMessageListsView();
		} else {
			super.onBackPressed();
		}
	}

//> PRIVATE HELPERS
	private void backToMessageListsView() {
		startActivity(new Intent(this, MessageListsActivity.class));
		finish();
	}

	private boolean saveSettings(Settings s) {
		try {
			SettingsStore.in(this).save(s);
			return true;
		} catch(IllegalSettingsException ex) {
			logException(ex, "SettingsDialogActivity.saveSettings()");
			for(IllegalSetting error : ex.errors) {
				showError(error);
			}
			return false;
		} catch(SettingsException ex) {
			logException(ex, "SettingsDialogActivity.saveSettings()");
			submitButton().setError(ex.getMessage());
			return false;
		}
	}

	private void startApp() {
		AsyncTask.execute(new Runnable() {
			public void run() {
				AlarmListener.restart(SettingsDialogActivity.this);
			}
		});
		startActivity(new Intent(this, MessageListsActivity.class));
		finish();
	}

	private Button cancelButton() {
		return (Button) findViewById(R.id.btnCancelSettings);
	}

	private Button submitButton() {
		return (Button) findViewById(R.id.btnSaveSettings);
	}

	private boolean checked(int componentId) {
		CheckBox field = (CheckBox) findViewById(componentId);
		return field.isChecked();
	}

	private void check(int componentId, boolean checked) {
		CheckBox field = (CheckBox) findViewById(componentId);
		field.setChecked(checked);
	}

	private String text(int componentId) {
		EditText field = (EditText) findViewById(componentId);
		return field.getText().toString();
	}

	private void text(int componentId, String value) {
		EditText field = (EditText) findViewById(componentId);
		field.setText(value);
	}

	private void showError(IllegalSetting error) {
		showError(error.componentId, error.errorStringId);
	}

	private void showError(int componentId, int stringId) {
		TextView field = (TextView) findViewById(componentId);
		field.setError(getString(stringId));
	}

	private ProgressDialog showSpinner(String message) {
		ProgressDialog p = new ProgressDialog(this);
		p.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		p.setMessage(message);
		p.setIndeterminate(true);
		p.setCanceledOnTouchOutside(false);
		p.show();
		return p;
	}

	private void log(String message, Object... extras) {
		trace(this, message, extras);
	}
}
