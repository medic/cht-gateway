package medic.gateway;

import android.test.*;

import java.util.concurrent.*;

import org.junit.*;

import okhttp3.mockwebserver.*;

import static org.junit.Assert.*;

@SuppressWarnings("PMD.SignatureDeclareThrowsException")
public class WebappUrlVerifierTest extends AndroidTestCase {
	private MockWebServer server;

	private WebappUrlVerifier verifier;

	@Before
	public void setUp() throws Exception {
		super.setUp();

		this.server = new MockWebServer();
		server.start();
		this.verifier = new WebappUrlVerifier();
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
		server.shutdown();
	}

	@Test
	public void test_verify_shouldReturnInvalidUrlFailureIfUrlNotValid() {
		// given
		String badUrl = "not-a-real-url";

		// when
		WebappUrlVerififcation v = verifier.verify(badUrl);

		// then
		assertEquals(badUrl, v.webappUrl);
		assertFalse(v.isOk);
		assertEquals(R.string.errInvalidUrl, v.failure);
	}

	@Test
	public void test_verify_shouldReturnOkResponseIfCorrectJsonReturned() {
		// given
		server.enqueue(new MockResponse().setBody("{\"medic-gateway\":true}"));

		// when
		WebappUrlVerififcation v = verifier.verify(serverUrl());

		// then
		assertEquals(serverUrl(), v.webappUrl);
		assertTrue(v.isOk);

		// and
		assertSingleGetRequestMade();
	}

	@Test
	public void test_verify_shouldReturnAppNotFoundFailureIfWrongJsonReturned() {
		// given
		server.enqueue(new MockResponse().setBody("{}"));

		// when
		WebappUrlVerififcation v = verifier.verify(serverUrl());

		// then
		assertEquals(serverUrl(), v.webappUrl);
		assertFalse(v.isOk);
		assertEquals(R.string.errWebappUrl_appNotFound, v.failure);

		// and
		assertSingleGetRequestMade();
	}

	@Test
	public void test_verify_shouldReturnUnauthorisedFailureIfServerReturnsAuthError() {
		// given
		server.enqueue(new MockResponse().setResponseCode(401));

		// when
		WebappUrlVerififcation v = verifier.verify(serverUrl());

		// then
		assertEquals(serverUrl(), v.webappUrl);
		assertFalse(v.isOk);
		assertEquals(R.string.errWebappUrl_unauthorised, v.failure);

		// and
		assertSingleGetRequestMade();
	}

	@Test
	public void test_verify_shouldReturnServerNotFoundFailureIfServerIsNotUp() throws Exception {
		// given
		server.shutdown();

		// when
		WebappUrlVerififcation v = verifier.verify(serverUrl());

		// then
		assertEquals(serverUrl(), v.webappUrl);
		assertFalse(v.isOk);
		assertEquals(R.string.errWebappUrl_serverNotFound, v.failure);
	}

//> PRIVATE HELPERS
	private String serverUrl() {
		return server.url("/api").toString();
	}

	private void assertSingleGetRequestMade() {
		RecordedRequest r = nextRequest();
		assertEquals("GET /api HTTP/1.1", r.getRequestLine());
		assertEquals("application/json", r.getHeader("Content-Type"));
		assertNull(nextRequest());
	}

	private RecordedRequest nextRequest() {
		try {
			return server.takeRequest(1, TimeUnit.MILLISECONDS);
		} catch(InterruptedException ex) {
			return null;
		}
	}
}
