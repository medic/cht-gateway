package medic.gateway;

import android.content.*;

import java.util.*;
import java.util.regex.*;

import static medic.gateway.BuildConfig.DEBUG;

public class SettingsStore {
	private final SharedPreferences prefs;

	SettingsStore(SharedPreferences prefs) {
		this.prefs = prefs;
	}

//> ACCESSORS
	public String getWebappUrl() { return get("app-url"); }

	private String get(String key) {
		return prefs.getString(key, null);
	}

	public Settings get() {
		Settings s = new Settings(getWebappUrl());
		try {
			s.validate();
		} catch(IllegalSettingsException ex) {
			return null;
		}
		return s;
	}

	public boolean allowsConfiguration() { return true; }

	public boolean hasSettings() {
		return get() != null;
	}

	public void save(Settings s) throws SettingsException {
		s.validate();

		SharedPreferences.Editor ed = prefs.edit();
		ed.putString("app-url", s.webappUrl);
		if(!ed.commit()) throw new SettingsException(
				"Failed to save to SharedPreferences.");
	}

	public static SettingsStore in(ContextWrapper ctx) {
		if(DEBUG) log("Loading settings for context %s...", ctx);

		SharedPreferences prefs = ctx.getSharedPreferences(
				SettingsStore.class.getName(),
				Context.MODE_PRIVATE);

		return new SettingsStore(prefs);
	}

	private static void log(String message, Object...extras) {
		if(DEBUG) System.err.println("LOG | SettingsStore :: " +
				String.format(message, extras));
	}
}

class Settings {
	public static final Pattern URL_PATTERN = Pattern.compile(
			"http[s]?://([^/:]*)(:\\d*)?(.*)");

	public final String webappUrl;

	public Settings(String webappUrl) {
		if(DEBUG) log("Settings() webappUrl=%s", webappUrl);
		this.webappUrl = webappUrl;
	}

	public void validate() throws IllegalSettingsException {
		List<IllegalSetting> errors = new LinkedList<>();

		if(!isSet(webappUrl)) {
			errors.add(new IllegalSetting(R.id.txtWebappUrl,
					R.string.errRequired));
		} else if(!URL_PATTERN.matcher(webappUrl).matches()) {
			errors.add(new IllegalSetting(R.id.txtWebappUrl,
					R.string.errInvalidUrl));
		}

		if(errors.size() > 0) {
			throw new IllegalSettingsException(errors);
		}
	}

	private boolean isSet(String val) {
		return val != null && val.length() > 0;
	}

	private void log(String message, Object...extras) {
		if(DEBUG) System.err.println("LOG | Settings :: " +
				String.format(message, extras));
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
