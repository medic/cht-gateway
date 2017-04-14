package medic.gateway.alert;

import android.content.Context;

import java.net.MalformedURLException;
import javax.net.ssl.SSLException;

import static medic.gateway.alert.BuildConfig.DISABLE_APP_URL_VALIDATION;
import static medic.gateway.alert.GatewayLog.logException;
import static medic.gateway.alert.R.string.errInvalidUrl;
import static medic.gateway.alert.R.string.errWebappUrl_appNotFound;
import static medic.gateway.alert.R.string.errWebappUrl_badSsl;
import static medic.gateway.alert.R.string.errWebappUrl_serverNotFound;
import static medic.gateway.alert.R.string.errWebappUrl_unauthorised;
import static medic.gateway.alert.SimpleJsonClient2.redactUrl;

public class WebappUrlVerifier {
	private final Context ctx;

	WebappUrlVerifier(Context ctx) {
		this.ctx = ctx;
	}

	public WebappUrlVerififcation verify(String webappUrl) {
		if(DISABLE_APP_URL_VALIDATION) {
			return WebappUrlVerififcation.ok(webappUrl);
		}

		try {
			SimpleResponse response = new SimpleJsonClient2().get(webappUrl);

			if(response instanceof JsonResponse && response.status < 400)
				return handleJsonResponse(webappUrl, (JsonResponse) response);
			else return handleFailResponse(webappUrl, response);
		} catch(MalformedURLException ex) {
			logException(ctx, ex, "Problem verifying url: %s", redactUrl(webappUrl));
			return WebappUrlVerififcation.failure(webappUrl, errInvalidUrl);
		}
	}

	private WebappUrlVerififcation handleJsonResponse(String webappUrl, JsonResponse response) {
		if(response.json.optBoolean("medic-gateway"))
			return WebappUrlVerififcation.ok(webappUrl);

		return WebappUrlVerififcation.failure(webappUrl, errWebappUrl_appNotFound);
	}

	private WebappUrlVerififcation handleFailResponse(String webappUrl, SimpleResponse response) {
		if(response instanceof ExceptionResponse) {
			ExceptionResponse exR = (ExceptionResponse) response;
			if(exR.ex instanceof SSLException) {
				return WebappUrlVerififcation.failure(webappUrl, errWebappUrl_badSsl);
			}
			logException(ctx, exR.ex, "Exception caught while trying to validate server URL: %s", redactUrl(webappUrl));
		}
		switch(response.status) {
			case 401:
				return WebappUrlVerififcation.failure(webappUrl, errWebappUrl_unauthorised);
			case -1:
				return WebappUrlVerififcation.failure(webappUrl, errWebappUrl_serverNotFound);
			default:
				return WebappUrlVerififcation.failure(webappUrl, errWebappUrl_appNotFound);
		}
	}
}

@SuppressWarnings("PMD.ShortMethodName")
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
