package medic.gateway;

import java.io.*;
import java.net.*;
import org.json.*;

import static medic.gateway.R.string.*;

import static medic.gateway.BuildConfig.DEBUG;
import static medic.gateway.BuildConfig.DISABLE_APP_URL_VALIDATION;

public class WebappUrlVerifier {
	public WebappUrlVerififcation verify(String webappUrl) {
		if(DISABLE_APP_URL_VALIDATION) {
			return WebappUrlVerififcation.ok(webappUrl);
		}

		try {
			JSONObject json = new SimpleJsonClient2().get(webappUrl);

			if(json.optBoolean("medic-gateway"))
				return WebappUrlVerififcation.ok(webappUrl);

			return WebappUrlVerififcation.failure(webappUrl, errWebappUrl_appNotFound);
		} catch(MalformedURLException ex) {
			// seems unlikely, as we should have verified this already
			return WebappUrlVerififcation.failure(webappUrl,
					errInvalidUrl);
		} catch(JSONException ex) {
			return WebappUrlVerififcation.failure(webappUrl,
					errWebappUrl_appNotFound);
		} catch(IOException ex) {
			if(DEBUG) ex.printStackTrace();
			return WebappUrlVerififcation.failure(webappUrl,
					errWebappUrl_serverNotFound);
		}
	}

	private boolean is200(String url) {
		if(DEBUG) log("is200() :: url=%s", url);
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) new URL(url).openConnection();
			return conn.getResponseCode() == 200;
		} catch (Exception ex) {
			if(DEBUG) ex.printStackTrace();
			return false;
		} finally {
			if(conn != null) try {
				conn.disconnect();
			} catch(Exception ex) {
				if(DEBUG) ex.printStackTrace();
			}
		}
	}

	private void log(String message, Object...extras) {
		if(DEBUG) System.err.println("LOG | WebappUrlVerifier::" +
				String.format(message, extras));
	}
}

class WebappUrlVerififcation {
	public final String webappUrl;
	public final boolean isOk;
	public final int failure;

	private WebappUrlVerififcation(String webappUrl, boolean isOk, int failure) {
		this.webappUrl = webappUrl;
		this.isOk = isOk;
		this.failure = failure;
	}

//> FACTORIES
	public static WebappUrlVerififcation ok(String webappUrl) {
		return new WebappUrlVerififcation(webappUrl, true, 0);
	}

	public static WebappUrlVerififcation failure(String webappUrl, int failure) {
		return new WebappUrlVerififcation(webappUrl, false, failure);
	}
}
