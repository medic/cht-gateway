package medic.gateway;

import android.util.*;

import java.io.*;
import java.net.*;

import org.json.*;

import static medic.gateway.BuildConfig.DEBUG;

/**
 * <p>New and improved - SimpleJsonClient2 is SimpleJsonClient, but using <code>
 * HttpURLConnection</code> instead of <code>DefaultHttpClient</code>.
 * <p>SimpleJsonClient2 should be used in preference to SimpleJsonClient on
 * Android 2.3 (API level 22/Gingerbread) and above.
 * @see java.net.HttpURLConnection
 * @see org.apache.http.impl.client.DefaultHttpClient
 */
public class SimpleJsonClient2 {
	static {
		// HTTP connection reuse which was buggy pre-froyo
		// TODO fix the commented bit!
//		if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO) {
			System.setProperty("http.keepAlive", "false");
//		}
	}

	public SimpleResponse get(String url) throws MalformedURLException {
		if(DEBUG) traceMethod("get", "url", url);
		return get(new URL(url));
	}

	public SimpleResponse get(URL url) {
		if(DEBUG) traceMethod("get", "url", url);
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
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
			StringBuilder bob = new StringBuilder();

			String line = null;
			while((line = reader.readLine()) != null) {
				bob.append(line + "\n");
			}
			String jsonString = bob.toString();
			if(DEBUG) log("get", "Retrieved JSON: " + jsonString);
			return new JsonResponse(conn.getResponseCode(),
					new JSONObject(jsonString));
		} catch (JSONException | IOException ex) {
			int responseCode = -1;
			try {
				responseCode = conn.getResponseCode();
			} catch(Exception ignore) {}
			return new ExceptionResponse(responseCode, ex);
		} finally {
			if(inputStream != null) try {
				inputStream.close();
			} catch(Exception ex) {
				if(DEBUG) ex.printStackTrace();
			}
			if(conn != null) try {
				conn.disconnect();
			} catch(Exception ex) {
				if(DEBUG) ex.printStackTrace();
			}
		}
	}

	public SimpleResponse post(String url, JSONObject content) throws MalformedURLException {
		if(DEBUG) traceMethod("post", "url", url);
		return post(new URL(url), content);
	}

	public SimpleResponse post(URL url, JSONObject content) {
		if(DEBUG) traceMethod("post", "url", url);
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
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
			StringBuilder bob = new StringBuilder();

			String line = null;
			while((line = reader.readLine()) != null) {
				bob.append(line + "\n");
			}
			String jsonString = bob.toString();
			if(DEBUG) log("post", "Retrieved JSON: " + jsonString);

			return new JsonResponse(conn.getResponseCode(),
					new JSONObject(jsonString));
		} catch (IOException | JSONException ex) {
			int responseCode = -1;
			try {
				responseCode = conn.getResponseCode();
			} catch(Exception ignore) {}
			return new ExceptionResponse(responseCode, ex);
		} finally {
			if(outputStream != null) try {
				outputStream.close();
			} catch(Exception ex) {
				if(DEBUG) ex.printStackTrace();
			}
			if(inputStream != null) try {
				inputStream.close();
			} catch(Exception ex) {
				if(DEBUG) ex.printStackTrace();
			}
			if(conn != null) try {
				conn.disconnect();
			} catch(Exception ex) {
				if(DEBUG) ex.printStackTrace();
			}
		}
	}

//> STATIC HELPERS
	private static HttpURLConnection openConnection(URL url) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		if(url.getUserInfo() != null) {
			conn.setRequestProperty("Authorization", "Basic " + base64encode(url.getUserInfo()));
		}
		return conn;
	}

	public static String base64encode(String normal) {
		try {
			return Base64.encodeToString(normal.getBytes("UTF-8"), Base64.DEFAULT);
		} catch(UnsupportedEncodingException ex) {
			// this should never happen on android
			throw new RuntimeException(ex);
		}
	}

	private static void traceMethod(String methodName, Object...args) {
		StringBuilder bob = new StringBuilder();
		for(int i=0; i<args.length; i+=2) {
			bob.append(args[i]);
			bob.append("=");
			bob.append(args[i+1]);
			bob.append(";");
		}
		log(methodName, bob.toString());
	}

	private static void log(String methodName, String message) {
		if(DEBUG) System.err.println("LOG | SimpleJsonClient2." +
				methodName + "()" +
				message);
	}
}

abstract class SimpleResponse {
	final int status;

	SimpleResponse(int status) {
		this.status = status;
	}

	boolean isError() {
		return this.status >= 400;
	}
}

class ExceptionResponse extends SimpleResponse {
	final Exception ex;

	ExceptionResponse(int status, Exception ex) {
		super(status);
		this.ex = ex;
	}

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
