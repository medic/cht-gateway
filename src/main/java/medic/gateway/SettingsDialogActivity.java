package medic.gateway;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;

import java.util.*;

import static medic.gateway.BuildConfig.DEBUG;
import static medic.gateway.GatewayLog.*;

public class SettingsDialogActivity extends Activity {
	private SettingsStore settings;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		log("Starting...");

		this.settings = SettingsStore.in(this);

		setContentView(R.layout.settings_dialog);

		if(!this.settings.hasSettings()) {
			cancelButton().setVisibility(View.GONE);
		}

		text(R.id.txtWebappUrl, settings.getWebappUrl());
		check(R.id.cbxEnablePolling, settings.isPollingEnabled());
	}

//> EVENT HANDLERS
	public void verifyAndSave(View view) {
		log("verifyAndSave");

		submitButton().setEnabled(false);
		cancelButton().setEnabled(false);

		String webappUrl = text(R.id.txtWebappUrl);
		final boolean syncEnabled = checked(R.id.cbxEnablePolling);

		new AsyncTask<String, Void, WebappUrlVerififcation>() {
			protected WebappUrlVerififcation doInBackground(String... webappUrl) {
				if(DEBUG && webappUrl.length != 1) throw new AssertionError();
				return new WebappUrlVerifier().verify(webappUrl[0]);
			}
			protected void onPostExecute(WebappUrlVerififcation result) {
				if(result.isOk) {
					boolean savedOk = saveSettings(new Settings(result.webappUrl, syncEnabled));
					if(savedOk) start();
				} else {
					showError(R.id.txtWebappUrl, result.failure);
					submitButton().setEnabled(true);
					cancelButton().setEnabled(true);
				}
			}
		}.execute(webappUrl);
	}

	public void cancelSettingsEdit(View view) {
		log("cancelSettingsEdit");
		backToMessageListsView();
	}

	public void onBackPressed() {
		if(this.settings.hasSettings()) {
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
			settings.save(s);
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

	private void start() {
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

	private void log(String message, Object... extras) {
		trace(this, message, extras);
	}
}
