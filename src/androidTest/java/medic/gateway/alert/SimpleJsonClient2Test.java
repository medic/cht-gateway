package medic.gateway.alert;

import android.test.*;

import okhttp3.mockwebserver.*;

import java.util.concurrent.TimeUnit;

import org.junit.*;

import static org.junit.Assert.*;

@SuppressWarnings("PMD.SignatureDeclareThrowsException")
public class SimpleJsonClient2Test {
	private MockWebServer server;

//> TEST SETUP/TEARDOWN
	@Before
	public void setUp() throws Exception {
		server = new MockWebServer();
		server.start();
		// server hangs without a response queued:
		server.enqueue(new MockResponse());
	}

	@After
	public void tearDown() throws Exception {
		server.shutdown();
	}

	@Test
	public void test_basicAuth_simplePassword() throws Exception {
		// given
		String url = String.format("http://uname:pword@localhost:%s/some-path", server.getPort());

		// when
		new SimpleJsonClient2().get(url);

		// then
		RecordedRequest r = server.takeRequest(1, TimeUnit.MILLISECONDS);
		assertEquals("GET /some-path HTTP/1.1", r.getRequestLine());
		assertEquals("Basic dW5hbWU6cHdvcmQ=", r.getHeader("Authorization"));
	}
}
