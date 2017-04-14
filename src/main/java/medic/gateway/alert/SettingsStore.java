package medic.gateway.alert;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import static medic.gateway.alert.BuildConfig.DEBUG;
import static medic.gateway.alert.BuildConfig.LOG_TAG;
import static medic.gateway.alert.GatewayLog.trace;
import static medic.gateway.alert.SimpleJsonClient2.redactUrl;

@SuppressWarnings("PMD.ShortMethodName")
public class SettingsStore {
	private final SharedPreferences prefs;

	SettingsStore(SharedPreferences prefs) {
		this.prefs = prefs;
	}

//> ACCESSORS
	public Settings get() {
		Settings s = new Settings(
				prefs.getString("app-url", null),
				prefs.getBoolean("polling-enabled", true),
				prefs.getBoolean("cdma-compat-enabled", false));

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
		ed.putBoolean("cdma-compat-enabled", s.cdmaCompatMode);
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

	public static final long POLL_INTERVAL = 30 * 1000L;

	public final String webappUrl;
	public final boolean pollingEnabled;
	public final boolean cdmaCompatMode;

	public Settings(String webappUrl, boolean pollingEnabled, boolean cdmaCompatMode) {
		log("Settings() webappUrl=%s", redactUrl(webappUrl));
		this.webappUrl = webappUrl;
		this.pollingEnabled = pollingEnabled;
		this.cdmaCompatMode = cdmaCompatMode;
	}

//> PUBLIC
	public void validate() throws IllegalSettingsException {
		if(!pollingEnabled) return;

		List<IllegalSetting> errors = new LinkedList<>();

		if(!isSet(webappUrl)) {
			errors.add(new IllegalSetting("txtWebappUrl:errRequired",
					R.id.txtWebappUrl,
					R.string.errRequired));
		} else if(!URL_PATTERN.matcher(webappUrl).matches()) {
			errors.add(new IllegalSetting("txtWebappUrl:errInvalidUrl:" + webappUrl,
					R.id.txtWebappUrl,
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
	public final String description;
	public final int componentId;
	public final int errorStringId;

	public IllegalSetting(String description, int componentId, int errorStringId) {
		this.description = description;
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
				bob.append(e.description);
			}
			return bob.toString();
		}
		return null;
	}
}
