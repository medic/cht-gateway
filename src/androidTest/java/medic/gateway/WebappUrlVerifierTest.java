package medic.gateway;

import android.test.*;

import medic.gateway.test.*;

import okhttp3.mockwebserver.*;

import org.junit.*;

import static org.junit.Assert.*;

@SuppressWarnings("PMD.SignatureDeclareThrowsException")
public class WebappUrlVerifierTest extends AndroidTestCase {
	private WebappUrlVerifier verifier;

	private HttpTestHelper http;

	@Before
	public void setUp() throws Exception {
		super.setUp();

		http = new HttpTestHelper();
		http.configureAppSettings(getContext());

		verifier = new WebappUrlVerifier();
	}

	@After
	public void tearDown() throws Exception {
		http.tearDown();
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
		http.nextResponseJson("{\"medic-gateway\":true}");

		// when
		WebappUrlVerififcation v = verifier.verify(http.url());

		// then
		assertEquals(http.url(), v.webappUrl);
		assertTrue(v.isOk);

		// and
		http.assertSingleGetRequestMade();
	}

	@Test
	public void test_verify_shouldReturnAppNotFoundFailureIfWrongJsonReturned() {
		// given
		http.nextResponseJson("{}");

		// when
		WebappUrlVerififcation v = verifier.verify(http.url());

		// then
		assertEquals(http.url(), v.webappUrl);
		assertFalse(v.isOk);
		assertEquals(R.string.errWebappUrl_appNotFound, v.failure);

		// and
		http.assertSingleGetRequestMade();
	}

	@Test
	public void test_verify_shouldReturnUnauthorisedFailureIfServerReturnsAuthError() {
		// given
		http.nextResponseError(401);

		// when
		WebappUrlVerififcation v = verifier.verify(http.url());

		// then
		assertEquals(http.url(), v.webappUrl);
		assertFalse(v.isOk);
		assertEquals(R.string.errWebappUrl_unauthorised, v.failure);

		// and
		http.assertSingleGetRequestMade();
	}

	@Test
	public void test_verify_shouldReturnServerNotFoundFailureIfServerIsNotUp() throws Exception {
		// given
		http.server.shutdown();

		// when
		WebappUrlVerififcation v = verifier.verify(http.url());

		// then
		assertEquals(http.url(), v.webappUrl);
		assertFalse(v.isOk);
		assertEquals(R.string.errWebappUrl_serverNotFound, v.failure);
	}
}
