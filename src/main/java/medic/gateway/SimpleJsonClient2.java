package medic.gateway;

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
//		if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO) {
			System.setProperty("http.keepAlive", "false");
//		}
	}

	public JSONObject get(String url) throws MalformedURLException, JSONException, IOException {
		if(DEBUG) traceMethod("get", "url", url);
		return get(new URL(url));
	}

	public JSONObject get(URL url) throws JSONException, IOException {
		if(DEBUG) traceMethod("get", "url", url);
		HttpURLConnection conn = null;
		InputStream inputStream = null;
		try {
			conn = (HttpURLConnection) url.openConnection();
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
			return new JSONObject(jsonString);
		} catch (JSONException | IOException ex) {
			throw ex;
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
