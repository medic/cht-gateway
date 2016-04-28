package medic.gateway;

import android.test.*;

import medic.gateway.test.*;

import okhttp3.mockwebserver.*;

import org.json.*;
import org.junit.*;
import java.util.regex.*;

import static org.junit.Assert.*;

@SuppressWarnings({"PMD.SignatureDeclareThrowsException", "PMD.JUnitTestsShouldIncludeAssert"})
public class WebappPollerTest extends HttpTestCase {
	private static final Pattern ANY_NUMBER = Pattern.compile("\\d+");

	private WebappPoller poller;

	@Before
	public void setUp() throws Exception {
		super.setUp();

		this.poller = new WebappPoller(getContext());
	}

	@Test
	public void test_pollWebapp_shouldFailQuietlyResponseIsNotJson() throws Exception {
		// given
		nextResponseJson("muhahaha not really json! {}");

		// when
		poller.pollWebapp();

		// then
		dbTableEmpty("wo_message");
	}

	@Test
	public void test_pollWebapp_shouldSaveMessagesFromResponseToDb() throws Exception {
		// given
		dbTableEmpty("wo_message");
		nextResponseJson("{ \"messages\": [ " +
					"{ \"id\": \"aaa-111\", \"to\": \"+1\", \"content\": \"testing: one\" }," +
					"{ \"id\": \"aaa-222\", \"to\": \"+2\", \"content\": \"testing: two\" }" +
				"] }");

		// when
		poller.pollWebapp();

		// then
		dbTableContains("wo_message",
				"aaa-111", "UNSENT", false, ANY_NUMBER, "+1", "testing: one",
				"aaa-222", "UNSENT", false, ANY_NUMBER, "+2", "testing: two");
	}

	@Test
	public void test_pollWebapp_poorlyFormedWoMessagesShouldNotAffectWellFormed() throws Exception {
		// given
		dbTableEmpty("wo_message");
		nextResponseJson("{ \"messages\": [ " +
					"{ \"id\": \"ok-111\", \"to\": \"+1\", \"content\": \"ok: one\" }," +

					// no id
					"{ \"to\": \"+1\", \"content\": \"bad\" }," +

					// no to
					"{ \"id\": \"bad-111\", \"content\": \"bad\" }," +

					// no content
					"{ \"id\": \"bad-222\", \"to\": \"+1\" }," +

					// no id or to
					"{ \"content\": \"bad\" }," +

					// no id or content
					"{ \"to\": \"+1\" }," +

					// no content or to
					"{ \"id\": \"bad-333\" }," +

					"{ \"id\": \"ok-222\", \"to\": \"+2\", \"content\": \"ok: two\" }" +
				"] }");

		// when
		poller.pollWebapp();

		// then
		dbTableContains("wo_message",
				"ok-111", "UNSENT", false, ANY_NUMBER, "+1", "ok: one",
				"ok-222", "UNSENT", false, ANY_NUMBER, "+2", "ok: two");
	}
}
