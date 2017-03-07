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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static medic.gateway.alert.BuildConfig.IS_MEDIC_FLAVOUR;
import static medic.gateway.alert.GatewayLog.logEvent;
import static medic.gateway.alert.GatewayLog.logException;
import static medic.gateway.alert.GatewayLog.trace;
import static medic.gateway.alert.Utils.redactUrl;
import static medic.gateway.alert.Utils.showSpinner;

@SuppressWarnings({"PMD.GodClass", "PMD.TooManyMethods"})
public class SettingsDialogActivity extends Activity {
	private static final String MEDIC_HOST_SUFFIX = ".medicmobile.org";
	private static final String MEDIC_URL_FORMATTER = "https://%s:%s@%s.medicmobile.org/api/sms";
	private static final Pattern MEDIC_URL_PARSER = Pattern.compile("https://([^:]+):([^:]+)@(.+).medicmobile.org/api/sms");

	private boolean hasPreviousSettings;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		log("Starting...");

		SettingsStore store = SettingsStore.in(this);
		hasPreviousSettings = store.hasSettings();

		setContentView(IS_MEDIC_FLAVOUR ? R.layout.settings_dialog_medic : R.layout.settings_dialog_generic);

		if(hasPreviousSettings) {
			Settings settings = store.get();

			populateWebappUrlFields(settings.getWebappUrl());
			check(R.id.cbxEnablePolling, settings.isPollingEnabled());
		} else {
			cancelButton().setVisibility(View.GONE);
		}
	}

//> EVENT HANDLERS
	public void doSave(View view) {
		log("doSave");

		boolean syncEnabled = checked(R.id.cbxEnablePolling);

		if(syncEnabled && requiredFieldsMissing()) return;

		submitButton().setEnabled(false);
		cancelButton().setEnabled(false);

		if(syncEnabled) {
			verifyAndSave();
		} else saveWithoutVerification();
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
	private boolean requiredFieldsMissing() {
		if(IS_MEDIC_FLAVOUR) {
			boolean hasBasicErrors = false;

			if(isBlank(R.id.txtWebappInstanceName)) {
				showError(R.id.txtWebappInstanceName, R.string.errRequired);
				hasBasicErrors = true;
			} else if(!text(R.id.txtWebappInstanceName).matches("^[\\w-_]+(\\.[\\w-_]+)*$")) {
				showError(R.id.txtWebappInstanceName, R.string.errInvalidInstanceName);
				hasBasicErrors = true;
			}

			if(isBlank(R.id.txtWebappUsername)) {
				showError(R.id.txtWebappUsername, R.string.errRequired);
				hasBasicErrors = true;
			}

			if(isBlank(R.id.txtWebappPassword)) {
				showError(R.id.txtWebappPassword, R.string.errRequired);
				hasBasicErrors = true;
			}

			return hasBasicErrors;
		} else {
			if(isBlank(R.id.txtWebappUrl)) {
				showError(R.id.txtWebappUrl, R.string.errRequired);
				return true;
			}
			return false;
		}
	}

	private String getWebappUrlFromFields() {
		if(IS_MEDIC_FLAVOUR) {
			String domain = text(R.id.txtWebappInstanceName);
			String username = text(R.id.txtWebappUsername);
			String password = text(R.id.txtWebappPassword);
			return String.format(MEDIC_URL_FORMATTER, username, password, domain);
		} else return text(R.id.txtWebappUrl);
	}

	private void populateWebappUrlFields(String appUrl) {
		if(IS_MEDIC_FLAVOUR) {
			Matcher m = MEDIC_URL_PARSER.matcher(appUrl);
			if(m.matches()) {
				text(R.id.txtWebappInstanceName, m.group(3));
				text(R.id.txtWebappUsername, m.group(1));
				text(R.id.txtWebappPassword, m.group(2));
			} else {
				trace(this, "URL not being parsed correctly: %s", appUrl);
			}
		} else text(R.id.txtWebappUrl, appUrl);
	}

	private void backToMessageListsView() {
		startActivity(new Intent(this, MessageListsActivity.class));
		finish();
	}

	private void verifyAndSave() {
		final String webappUrl = getWebappUrlFromFields();

		final ProgressDialog spinner = showSpinner(this,
				String.format(getString(R.string.txtValidatingWebappUrl),
						webappUrl));

		new AsyncTask<Void, Void, WebappUrlVerififcation>() {
			protected WebappUrlVerififcation doInBackground(Void..._) {
				return new WebappUrlVerifier().verify(webappUrl);
			}
			protected void onPostExecute(WebappUrlVerififcation result) {
				boolean savedOk = false;

				if(result.isOk)
					savedOk = saveSettings(new Settings(result.webappUrl, true));
				else
					showError(IS_MEDIC_FLAVOUR ? R.id.txtWebappInstanceName : R.id.txtWebappUrl, result.failure);

				if(savedOk) startApp();
				else {
					submitButton().setEnabled(true);
					cancelButton().setEnabled(true);
				}
				spinner.dismiss();
			}
		}.execute();
	}

	private void saveWithoutVerification() {
		final String webappUrl = getWebappUrlFromFields();

		final ProgressDialog spinner = showSpinner(this,
				getString(R.string.txtSavingSettings));

		AsyncTask.execute(new Runnable() {
			public void run() {
				boolean savedOk = saveSettings(new Settings(webappUrl, false));

				if(savedOk) startApp();
				else {
					runOnUiThread(new Runnable() {
						public void run() {
							submitButton().setEnabled(true);
							cancelButton().setEnabled(true);
						}
					});
				}
				spinner.dismiss();
			}
		});
	}

	private boolean saveSettings(Settings s) {
		try {
			SettingsStore.in(this).save(s);
			logEvent(SettingsDialogActivity.this, "Settings saved.  Webapp URL: %s", redactUrl(s.getWebappUrl()));
			return true;
		} catch(final IllegalSettingsException ex) {
			logException(ex, "SettingsDialogActivity.saveSettings()");
			runOnUiThread(new Runnable() {
				public void run() {
					for(IllegalSetting error : ex.errors) {
						showError(error);
					}
				}
			});
			return false;
		} catch(final SettingsException ex) {
			logException(ex, "SettingsDialogActivity.saveSettings()");
			runOnUiThread(new Runnable() {
				public void run() {
					submitButton().setError(ex.getMessage());
				}
			});
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

	private boolean isBlank(int componentId) {
		return text(componentId).length() == 0;
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
