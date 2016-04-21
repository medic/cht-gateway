package medic.gateway;

import java.io.*;
import java.net.*;
import org.json.*;

import static medic.gateway.R.string.*;

import static medic.gateway.BuildConfig.DEBUG;
import static medic.gateway.BuildConfig.DISABLE_APP_URL_VALIDATION;
import static medic.gateway.GatewayLog.*;

public class WebappUrlVerifier {
	public WebappUrlVerififcation verify(String webappUrl) {
		if(DISABLE_APP_URL_VALIDATION) {
			return WebappUrlVerififcation.ok(webappUrl);
		}

		try {
			SimpleResponse response = new SimpleJsonClient2().get(webappUrl);

			if(DEBUG) {
				log("##############################################");
				log("# " + response);
				log("##############################################");
			}

			if(response instanceof JsonResponse && response.status < 400)
				return handleJsonResponse(webappUrl, (JsonResponse) response);
			else return handleFailResponse(webappUrl, response);
		} catch(MalformedURLException ex) {
			return WebappUrlVerififcation.failure(webappUrl, errInvalidUrl);
		}
	}

	private WebappUrlVerififcation handleJsonResponse(String webappUrl, JsonResponse response) {
		if(response.json.optBoolean("medic-gateway"))
			return WebappUrlVerififcation.ok(webappUrl);

		return WebappUrlVerififcation.failure(webappUrl, errWebappUrl_appNotFound);
	}

	private WebappUrlVerififcation handleFailResponse(String webappUrl, SimpleResponse response) {
		switch(response.status) {
			case 401:
				return WebappUrlVerififcation.failure(webappUrl, errWebappUrl_unauthorised);
			case -1:
				return WebappUrlVerififcation.failure(webappUrl, errWebappUrl_serverNotFound);
			default:
				return WebappUrlVerififcation.failure(webappUrl, errWebappUrl_appNotFound);
		}
	}

	private void log(String message, Object...extras) {
		trace(this, message, extras);
	}
}

@SuppressWarnings("PMD")
final class WebappUrlVerififcation {
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
