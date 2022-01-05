package medic.gateway.alert;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.List;
import static medic.gateway.alert.BuildConfig.DEBUG;
import static medic.gateway.alert.GatewayLog.trace;

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
				prefs.getBoolean("cdma-compat-enabled", false),
				prefs.getBoolean("dummy-send-enabled", false));

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
		ed.putBoolean("dummy-send-enabled", s.dummySendMode);
		if(!ed.commit()) throw new SettingsException(
				"Failed to save to SharedPreferences.");
	}

	public static SettingsStore in(Context ctx) {
		trace(SettingsStore.class, "loading for context: %s", ctx);

		SharedPreferences prefs = ctx.getSharedPreferences(
				SettingsStore.class.getName(),
				Context.MODE_PRIVATE);

		return new SettingsStore(prefs);
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
