package medic.gateway;

import medic.gateway.test.*;

import okhttp3.mockwebserver.*;

import org.junit.*;

import static org.junit.Assert.*;

@SuppressWarnings("PMD.SignatureDeclareThrowsException")
public class WebappUrlVerifierTest extends HttpTestCase {
	private WebappUrlVerifier verifier;

	@Before
	public void setUp() throws Exception {
		super.setUp();

		this.verifier = new WebappUrlVerifier();
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
		nextResponseJson("{\"medic-gateway\":true}");

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
		nextResponseJson("{}");

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
		nextResponseError(401);

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
}
