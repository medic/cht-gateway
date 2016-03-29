package medic.gateway;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;

import java.util.*;

import static medic.gateway.BuildConfig.DEBUG;
import static android.view.View.GONE;

public class SettingsDialogActivity extends Activity {
	private SettingsStore settings;

	public void onCreate(Bundle savedInstanceState) {
		if(DEBUG) log("Starting...");
		super.onCreate(savedInstanceState);

		this.settings = SettingsStore.in(this);

		setContentView(R.layout.settings_dialog);

		if(!this.settings.hasSettings()) {
			cancelButton().setVisibility(View.GONE);
		}

		text(R.id.txtWebappUrl, settings.getWebappUrl());
	}

//> EVENT HANDLERS
	public void verifyAndSave(View view) {
		if(DEBUG) log("verifyAndSave");

		submitButton().setEnabled(false);
		cancelButton().setEnabled(false);

		String webappUrl = text(R.id.txtWebappUrl);

		new AsyncTask<String, Void, WebappUrlVerififcation>() {
			protected WebappUrlVerififcation doInBackground(String... webappUrl) {
				assert webappUrl.length == 1;
				return new WebappUrlVerifier().verify(webappUrl[0]);
			}
			protected void onPostExecute(WebappUrlVerififcation result) {
				if(result.isOk) {
					saveSettings(new Settings(result.webappUrl));
				} else {
					showError(R.id.txtWebappUrl, result.failure);
					submitButton().setEnabled(true);
					cancelButton().setEnabled(true);
				}
			}
		}.execute(webappUrl);
	}

	public void cancelSettingsEdit(View view) {
		if(DEBUG) log("cancelSettingsEdit");
		backToMessageListsView();
	}

//> PRIVATE HELPERS
	private void backToMessageListsView() {
		startActivity(new Intent(this, MessageListsActivity.class));
		finish();
	}

	private void saveSettings(Settings s) {
		try {
			settings.save(s);
			ServiceManager.restart(this);
			startActivity(new Intent(this, MessageListsActivity.class));
			finish();
		} catch(IllegalSettingsException ex) {
			if(DEBUG) ex.printStackTrace();
			for(IllegalSetting error : ex.errors) {
				showError(error);
			}
		} catch(SettingsException ex) {
			if(DEBUG) ex.printStackTrace();
			submitButton().setError(ex.getMessage());
		}
	}

	private Button cancelButton() {
		return (Button) findViewById(R.id.btnCancelSettings);
	}

	private Button submitButton() {
		return (Button) findViewById(R.id.btnSaveSettings);
	}

	private String text(int componentId) {
		EditText field = (EditText) findViewById(componentId);
		return field.getText().toString();
	}

	private void text(int componentId, String value) {
		EditText field = (EditText) findViewById(componentId);
		field.setText(value);
	}

	private void removeError(int componentId) {
		EditText field = (EditText) findViewById(componentId);
		field.setError(null);
	}

	private void showError(IllegalSetting error) {
		showError(error.componentId, error.errorStringId);
	}

	private void showError(int componentId, int stringId) {
		TextView field = (TextView) findViewById(componentId);
		field.setError(getString(stringId));
	}

	private void log(String message, Object...extras) {
		if(DEBUG) System.err.println("LOG | SettingsDialogActivity :: " +
				String.format(message, extras));
	}
}
