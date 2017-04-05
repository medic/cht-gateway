package medic.gateway.alert;

import android.os.Build;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import static medic.gateway.alert.BuildConfig.DEBUG;
import static medic.gateway.alert.BuildConfig.LOG_TAG;

/**
 * <p>New and improved - SimpleJsonClient2 is SimpleJsonClient, but using <code>
 * HttpURLConnection</code> instead of <code>DefaultHttpClient</code>.
 * <p>SimpleJsonClient2 should be used in preference to SimpleJsonClient on
 * Android 2.3 (API level 9/Gingerbread) and above.
 * @see java.net.HttpURLConnection
 * @see org.apache.http.impl.client.DefaultHttpClient
 */
@SuppressWarnings("PMD.GodClass")
public class SimpleJsonClient2 {
	static {
		// HTTP connection reuse which was buggy pre-froyo
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
			System.setProperty("http.keepAlive", "false");
		}
	}

	private static final Pattern AUTH_URL = Pattern.compile("(.+)://(.*):(.*)@(.*)");

//> PUBLIC METHODS
	public SimpleResponse get(String url) throws MalformedURLException {
		if(DEBUG) traceMethod("get", "url", redactUrl(url));
		return get(new URL(url));
	}

	public SimpleResponse get(URL url) {
		if(DEBUG) traceMethod("get", "url", redactUrl(url));
		HttpURLConnection conn = null;
		InputStream inputStream = null;
		try {
			conn = openConnection(url);
			conn.setRequestProperty("Content-Type", "application/json");

			if(conn.getResponseCode() < 400) {
				inputStream = conn.getInputStream();
			} else {
				inputStream = conn.getErrorStream();
			}

			return new JsonResponse(conn.getResponseCode(),
					jsonResponseFrom("get", inputStream));
		} catch (JSONException | IOException ex) {
			return exceptionResponseFor(conn, ex);
		} finally {
			closeSafely("get", inputStream);
			closeSafely("get", conn);
		}
	}

	public SimpleResponse post(String url, JSONObject content) throws MalformedURLException {
		if(DEBUG) traceMethod("post", "url", redactUrl(url));
		return post(new URL(url), content);
	}

	public SimpleResponse post(URL url, JSONObject content) {
		if(DEBUG) traceMethod("post", "url", redactUrl(url));
		HttpURLConnection conn = null;
		OutputStream outputStream = null;
		InputStream inputStream = null;
		try {
			conn = openConnection(url);
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestProperty("Accept-Charset", "utf-8");
			conn.setRequestProperty("Cache-Control", "no-cache");
			conn.setRequestProperty("Content-Type", "application/json");

			outputStream = conn.getOutputStream();
			outputStream.write(content.toString().getBytes("UTF-8"));

			if(conn.getResponseCode() < 400) {
				inputStream = conn.getInputStream();
			} else {
				inputStream = conn.getErrorStream();
			}

			return new JsonResponse(conn.getResponseCode(),
					jsonResponseFrom("post", inputStream));
		} catch (IOException | JSONException ex) {
			return exceptionResponseFor(conn, ex);
		} finally {
			closeSafely("post", outputStream);
			closeSafely("post", inputStream);
			closeSafely("post", conn);
		}
	}

//> PUBLIC UTILS
	public static String redactUrl(URL url) {
		return redactUrl(url.toString());
	}
	public static String redactUrl(String url) {
		if(url == null) return null;

		Matcher m = AUTH_URL.matcher(url);
		if(!m.matches()) return url;

		return String.format("%s://%s:%s@%s",
				m.group(1), m.group(2), "****", m.group(4));
	}

//> INSTANCE HELPERS
	private JSONObject jsonResponseFrom(String method, InputStream in) throws IOException, JSONException {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(in, "UTF-8"), 8);
			StringBuilder bob = new StringBuilder();

			String line = null;
			while((line = reader.readLine()) != null) {
				bob.append(line).append('\n');
			}
			String jsonString = bob.toString();
			if(DEBUG) log(method, "Retrieved JSON: %s", jsonString);
			return new JSONObject(jsonString);
		} finally {
			closeSafely(method, reader);
		}
	}

	private ExceptionResponse exceptionResponseFor(HttpURLConnection conn, Exception ex) {
		int responseCode = -1;
		try {
			responseCode = conn.getResponseCode();
		} catch(Exception ignore) {} // NOPMD
		return new ExceptionResponse(responseCode, ex);
	}

	private void closeSafely(String method, Closeable c) {
		if(c != null) try {
			c.close();
		} catch(Exception ex) {
			if(DEBUG) log(ex, "SimpleJsonClient2.%s()", method);
		}
	}

	private void closeSafely(String method, HttpURLConnection conn) {
		if(conn != null) try {
			conn.disconnect();
		} catch(Exception ex) {
			if(DEBUG) log(ex, "SimpleJsonClient2.%s()", method);
		}
	}

//> STATIC HELPERS
	@SuppressWarnings("PMD.PreserveStackTrace")
	private static HttpURLConnection openConnection(URL url) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		String userAgent = String.format("%s %s/%s",
				System.getProperty("http.agent"),
				BuildConfig.APPLICATION_ID,
				BuildConfig.VERSION_NAME);
		conn.setRequestProperty("User-Agent", userAgent);

		if(url.getUserInfo() != null) {
			try {
				conn.setRequestProperty("Authorization", "Basic " + encodeCredentials(url.getUserInfo()));
			} catch(Exception ex) {
				// Don't include exception details in case they include auth details
				throw new RuntimeException(String.format("%s caught while setting Authorization header.", ex.getClass()));
			}
		}
		return conn;
	}

	/**
	 * Base64-encode the {@code user-pass} component of HTTP {@code Authorization: Basic}
	 * header.  Note that ISO-8859-1 encoding is used, unless the characterset is
	 * unavailable, in which case UTF-8 is used instead.
	 *
	 * <strong>N.B. passwords with some special characters may not work.</strong>
	 *
	 * @see https://tools.ietf.org/html/rfc2617#section-2
	 */
	@SuppressWarnings("PMD.PreserveStackTrace")
	private static String encodeCredentials(String normal) {
		try {
			return Base64.encodeToString(normal.getBytes("ISO-8859-1"), Base64.NO_WRAP);
		} catch(UnsupportedEncodingException ignored) {
			Log.i(LOG_TAG, "UnsupportedEncodingException thrown trying to encode HTTP basic auth credentials with ISO-8859-1.  Will try UTF-8.");
			try {
				return Base64.encodeToString(normal.getBytes("UTF-8"), Base64.NO_WRAP);
			} catch(UnsupportedEncodingException why) {
				// this should never happen on android, as UTF-8 is always the default encoding
				throw new RuntimeException(why);
			}
		}
	}

	private static void traceMethod(String methodName, Object...args) {
		StringBuilder bob = new StringBuilder();
		for(int i=0; i<args.length; i+=2) {
			bob.append(';');
			bob.append(args[i]);
			bob.append('=');
			bob.append(args[i+1]);
		}
		log(methodName, bob.length() > 0 ? bob.substring(1) : "");
	}

	private static void log(String methodName, String message, Object... extras) {
		Log.d(LOG_TAG, "SimpleJsonClient2." + methodName + "() :: " + String.format(message, extras));
	}

	private static void log(Exception ex, String message, Object... extras) {
		Log.i(LOG_TAG, String.format(message, extras), ex);
	}
}

@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
abstract class SimpleResponse {
	final int status;

	SimpleResponse(int status) {
		this.status = status;
	}

	abstract boolean isError();
}

class ExceptionResponse extends SimpleResponse {
	final Exception ex;

	ExceptionResponse(int status, Exception ex) {
		super(status);
		this.ex = ex;
	}

	boolean isError() { return true; }

	public String toString() {
		return new StringBuilder()
				.append('[')
				.append(status)
				.append('|')
				.append(ex)
				.append(']')
				.toString();
	}
}

class JsonResponse extends SimpleResponse {
	final JSONObject json;

	JsonResponse(int status, JSONObject json) {
		super(status);
		this.json = json;
	}

	boolean isError() {
		return this.status < 200 || this.status >= 300;
	}

	public String toString() {
		return new StringBuilder()
				.append('[')
				.append(status)
				.append('|')
				.append(json)
				.append(']')
				.toString();
	}
}
