package medic.gateway.alert.test;

import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.test.*;

import java.util.concurrent.*;

import medic.gateway.alert.*;

import okhttp3.mockwebserver.*;

import org.json.*;
import org.junit.*;

import static org.junit.Assert.*;
import static medic.gateway.alert.test.TestUtils.*;

@SuppressWarnings({"PMD.JUnit4TestShouldUseAfterAnnotation",
		"PMD.SignatureDeclareThrowsException"})
public class HttpTestHelper {
	public MockWebServer server;

	public HttpTestHelper() throws Exception {
		server = new MockWebServer();
		server.start();
	}

//> TEST SETUP/TEARDOWN
	public void tearDown() throws Exception {
		server.shutdown();
	}

	public void configureAppSettings(Context ctx) {
		SharedPreferences.Editor ed = ctx
				.getSharedPreferences(SettingsStore.class.getName(), Context.MODE_PRIVATE)
				.edit();
		ed.putString("app-url", url());
		assertTrue(ed.commit());
	}

//> CONVENIENCE METHODS
	public String url() {
		return server.url("/api").toString();
	}

//> TEST HELPERS
	public void nextResponseJson(String jsonString) {
		server.enqueue(new MockResponse().setBody(jsonString));
	}

	public void nextResponseError(int httpResponseCode) {
		server.enqueue(new MockResponse().setResponseCode(httpResponseCode));
	}

	public RecordedRequest assertSingleGetRequestMade() {
		RecordedRequest r = nextRequest();
		assertEquals("GET /api HTTP/1.1", r.getRequestLine());
		assertEquals("application/json", r.getHeader("Content-Type"));
		assertNull(nextRequest());
		return r;
	}

	public JSONObject assertPostRequestMade_withJsonResponse() throws JSONException {
		return new JSONObject(assertPostRequestMade().getBody().readUtf8());
	}

	public RecordedRequest assertPostRequestMade() {
		RecordedRequest r = nextRequest();
		assertEquals("POST /api HTTP/1.1", r.getRequestLine());
		assertEquals("application/json", r.getHeader("Content-Type"));
		return r;
	}

	public void assertSinglePostRequestMade() {
		assertPostRequestMade();
		assertNoMoreRequests();
	}

	public void assertNoMoreRequests() {
		assertNull(nextRequest());
	}

	public RecordedRequest nextRequest() {
		try {
			return server.takeRequest(1, TimeUnit.MILLISECONDS);
		} catch(InterruptedException ex) {
			return null;
		}
	}
}
