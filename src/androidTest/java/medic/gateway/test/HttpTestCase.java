package medic.gateway.test;

import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.test.*;

import java.util.concurrent.*;

import medic.gateway.*;

import okhttp3.mockwebserver.*;

import org.json.*;
import org.junit.*;

import static org.junit.Assert.*;
import static medic.gateway.test.TestUtils.*;

public abstract class HttpTestCase extends AndroidTestCase {
	protected MockWebServer server;

	protected DbTestHelper db;

	@Before
	public void setUp() throws Exception {
		super.setUp();

		this.server = new MockWebServer();
		server.start();

		SharedPreferences.Editor ed = getContext()
				.getSharedPreferences(SettingsStore.class.getName(), Context.MODE_PRIVATE)
				.edit();
		ed.putString("app-url", serverUrl());
		assertTrue(ed.commit());

		db = new DbTestHelper(getContext());
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
		server.shutdown();
		db.tearDown();
	}

//> HTTP HELPERS
	protected void nextResponseJson(String jsonString) {
		server.enqueue(new MockResponse().setBody(jsonString));
	}

	protected void nextResponseError(int httpResponseCode) {
		server.enqueue(new MockResponse().setResponseCode(httpResponseCode));
	}

	protected String serverUrl() {
		return server.url("/api").toString();
	}

	protected void assertSingleGetRequestMade() {
		RecordedRequest r = nextRequest();
		assertEquals("GET /api HTTP/1.1", r.getRequestLine());
		assertEquals("application/json", r.getHeader("Content-Type"));
		assertNull(nextRequest());
	}

	protected JSONObject assertPostRequestMade_withJsonResponse() throws JSONException {
		return new JSONObject(assertPostRequestMade().getBody().readUtf8());
	}

	protected RecordedRequest assertPostRequestMade() {
		RecordedRequest r = nextRequest();
		assertEquals("POST /api HTTP/1.1", r.getRequestLine());
		assertEquals("application/json", r.getHeader("Content-Type"));
		return r;
	}

	protected void assertSinglePostRequestMade() {
		assertPostRequestMade();
		assertNoMoreRequests();
	}

	protected void assertNoMoreRequests() {
		assertNull(nextRequest());
	}

	protected RecordedRequest nextRequest() {
		try {
			return server.takeRequest(1, TimeUnit.MILLISECONDS);
		} catch(InterruptedException ex) {
			return null;
		}
	}
}
