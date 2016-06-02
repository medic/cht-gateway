package medic.gateway.alert;

import android.content.*;
import android.util.*;

import java.util.*;
import java.util.regex.*;

import static medic.gateway.alert.BuildConfig.DEBUG;
import static medic.gateway.alert.BuildConfig.LOG_TAG;
import static medic.gateway.alert.GatewayLog.*;

@SuppressWarnings("PMD.ShortMethodName")
public class SettingsStore {
	private final SharedPreferences prefs;

	SettingsStore(SharedPreferences prefs) {
		this.prefs = prefs;
	}

//> ACCESSORS
	public Settings get() {
		final String webappUrl = prefs.getString("app-url", null);
		final boolean pollingEnabled = prefs.getBoolean("polling-enabled", true);

		Settings s = new Settings(webappUrl, pollingEnabled);
		try {
			s.validate();
		} catch(IllegalSettingsException ex) {
			return null;
		}
		return s;
	}

	public boolean hasSettings() {
		return get() != null;
	}

	public void save(Settings s) throws SettingsException {
		s.validate();

		SharedPreferences.Editor ed = prefs.edit();
		ed.putString("app-url", s.webappUrl);
		ed.putBoolean("polling-enabled", s.pollingEnabled);
		if(!ed.commit()) throw new SettingsException(
				"Failed to save to SharedPreferences.");
	}

	public static SettingsStore in(Context ctx) {
		if(DEBUG) Log.d(LOG_TAG, String.format("SettingStore :: loading for context: %s", ctx));

		SharedPreferences prefs = ctx.getSharedPreferences(
				SettingsStore.class.getName(),
				Context.MODE_PRIVATE);

		return new SettingsStore(prefs);
	}
}

@SuppressWarnings("PMD.ShortMethodName")
class Settings {
	public static final Pattern URL_PATTERN = Pattern.compile(
			"http[s]?://([^/:]*)(:\\d*)?(.*)");

	public final String webappUrl;
	public final boolean pollingEnabled;

	public Settings(String webappUrl, boolean pollingEnabled) {
		log("Settings() webappUrl=%s", webappUrl);
		this.webappUrl = webappUrl;
		this.pollingEnabled = pollingEnabled;
	}

//> ACCESSORS
	public String getWebappUrl() { return webappUrl; }

	public boolean isPollingEnabled() { return pollingEnabled; }

	public long getPollInterval() { return 30 * 1000L; }

//> PUBLIC
	public void validate() throws IllegalSettingsException {
		List<IllegalSetting> errors = new LinkedList<>();

		if(!isSet(webappUrl)) {
			errors.add(new IllegalSetting(R.id.txtWebappUrl,
					R.string.errRequired));
		} else if(!URL_PATTERN.matcher(webappUrl).matches()) {
			errors.add(new IllegalSetting(R.id.txtWebappUrl,
					R.string.errInvalidUrl));
		}

		if(!errors.isEmpty()) {
			throw new IllegalSettingsException(errors);
		}
	}

//> PRIVATE HELPERS
	private boolean isSet(String val) {
		return val != null && val.length() > 0;
	}

	private void log(String message, Object...extras) {
		trace(this, message, extras);
	}

//> FACTORIES
	public static Settings in(Context ctx) {
		return SettingsStore.in(ctx).get();
	}
}

class IllegalSetting {
	public final int componentId;
	public final int errorStringId;

	public IllegalSetting(int componentId, int errorStringId) {
		this.componentId = componentId;
		this.errorStringId = errorStringId;
	}
}

class SettingsException extends Exception {
	public SettingsException(String message) {
		super(message);
	}
}

class IllegalSettingsException extends SettingsException {
	public final List<IllegalSetting> errors;

	public IllegalSettingsException(List<IllegalSetting> errors) {
		super(createMessage(errors));
		this.errors = errors;
	}

	private static String createMessage(List<IllegalSetting> errors) {
		if(DEBUG) {
			StringBuilder bob = new StringBuilder();
			for(IllegalSetting e : errors) {
				if(bob.length() > 0) bob.append("; ");
				bob.append(String.format(
						"component[%s]: error[%s]", e.componentId, e.errorStringId));
			}
			return bob.toString();
		}
		return null;
	}
}
