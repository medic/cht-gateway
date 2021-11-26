package medic.gateway.alert;

import android.content.Context;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import static medic.gateway.alert.GatewayLog.trace;
import static medic.gateway.alert.SimpleJsonClient2.redactUrl;

@SuppressWarnings("PMD.ShortMethodName")
public class Settings {
	public static final Pattern URL_PATTERN = Pattern.compile(
			"http[s]?://([^/:]*)(:\\d*)?(.*)");

	public static final long POLL_INTERVAL = 30 * 1000L;

	public final String webappUrl;
	public final boolean pollingEnabled;
	public final boolean cdmaCompatMode;
	public final boolean dummySendMode;

	public Settings(String webappUrl, boolean pollingEnabled, boolean cdmaCompatMode, boolean dummySendMode) {
		trace(this, "Settings() webappUrl=%s", redactUrl(webappUrl));
		this.webappUrl = webappUrl;
		this.pollingEnabled = pollingEnabled;
		this.cdmaCompatMode = cdmaCompatMode;
		this.dummySendMode = dummySendMode;
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

//> FACTORIES
	public static Settings in(Context ctx) {
		return SettingsStore.in(ctx).get();
	}
}
