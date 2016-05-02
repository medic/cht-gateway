package medic.gateway;

import android.test.*;

import medic.gateway.test.*;

import okhttp3.mockwebserver.*;

import org.json.*;
import org.junit.*;

import static org.junit.Assert.*;
import static medic.gateway.test.DbTestHelper.*;
import static medic.gateway.test.TestUtils.*;

@SuppressWarnings({"PMD.SignatureDeclareThrowsException", "PMD.JUnitTestsShouldIncludeAssert"})
public class WebappPollerTest extends AndroidTestCase {
	private WebappPoller poller;

	private DbTestHelper db;
	private HttpTestHelper http;

	@Before
	public void setUp() throws Exception {
		super.setUp();

		db = new DbTestHelper(getContext());

		http = new HttpTestHelper();
		http.configureAppSettings(getContext());

		poller = new WebappPoller(getContext());
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();

		http.tearDown();
		db.tearDown();

		http.assertNoMoreRequests();
	}

//> REQUEST CONTENT TESTS
	@Test
	public void test_pollWebapp_shouldOnlyIncludeWaitingMessagesInRequest() throws Exception {
		// given
		String waitingId = randomUuid();
		db.insert("wt_message",
				cols("_id", "status", "last_action", "_from", "content"),
				vals(waitingId, WtMessage.Status.WAITING, 0, A_PHONE_NUMBER, SOME_CONTENT));
		db.insert("wt_message",
				cols("_id", "status", "last_action", "_from", "content"),
				vals(randomUuid(), WtMessage.Status.FORWARDED, 0, A_PHONE_NUMBER, SOME_CONTENT));
		db.insert("wt_message",
				cols("_id", "status", "last_action", "_from", "content"),
				vals(randomUuid(), WtMessage.Status.FAILED, 0, A_PHONE_NUMBER, SOME_CONTENT));
		http.nextResponseJson("{}");

		// when
		poller.pollWebapp();

		// then
		JSONObject responseBody = http.assertPostRequestMade_withJsonResponse();

		JSONArray messages = responseBody.getJSONArray("messages");
		assertEquals(1, messages.length());

		JSONObject message = (JSONObject) messages.get(0);
		assertEquals(waitingId, message.getString("id"));
		assertEquals(A_PHONE_NUMBER, message.getString("from"));
		assertEquals(SOME_CONTENT, message.getString("content"));
	}

	@Test
	public void test_pollWebapp_shouldOnlyIncludeStatusUpdatesThatNeedForwarding() throws Exception {
		// given
		String deliveredId = randomUuid();
		db.insert("wo_message",
				cols("_id", "status", "status_needs_forwarding", "last_action", "_to", "content"),
				vals(deliveredId, WoMessage.Status.DELIVERED, true, 0, A_PHONE_NUMBER, SOME_CONTENT));
		db.insert("wo_message",
				cols("_id", "status", "status_needs_forwarding", "last_action", "_to", "content"),
				vals(randomUuid(), WoMessage.Status.DELIVERED, false, 0, A_PHONE_NUMBER, SOME_CONTENT));
		http.nextResponseJson("{}");

		// when
		poller.pollWebapp();

		// then
		JSONObject responseBody = http.assertPostRequestMade_withJsonResponse();

		JSONArray messages = responseBody.getJSONArray("deliveries");
		assertEquals(1, messages.length());

		JSONObject message = (JSONObject) messages.get(0);
		assertEquals(deliveredId, message.getString("id"));
		assertEquals("DELIVERED", message.getString("status"));
	}

//> RESPONSE CONTENT TESTS
	@Test
	public void test_pollWebapp_shouldFailQuietlyResponseIsNotJson() throws Exception {
		// given
		http.nextResponseJson("muhahaha not really json! {}");

		// when
		poller.pollWebapp();

		// then
		http.assertSinglePostRequestMade();
		db.assertEmpty("wo_message");
	}

	@Test
	public void test_pollWebapp_shouldBeFineIfMessagesIsNotIncludedInResponse() throws Exception {
		// given
		http.nextResponseJson("{}");

		// when
		poller.pollWebapp();

		// then
		http.assertSinglePostRequestMade();
		db.assertEmpty("wo_message");
	}

	@Test
	public void test_pollWebapp_shouldBeFineIfMessagesIsNull() throws Exception {
		// given
		http.nextResponseJson("{ \"messages\":null }");

		// when
		poller.pollWebapp();

		// then
		http.assertSinglePostRequestMade();
		db.assertEmpty("wo_message");
	}

	@Test
	public void test_pollWebapp_shouldBeFineIfNoMessagesInResponse() throws Exception {
		// given
		http.nextResponseJson("{ \"messages\":[] }");

		// when
		poller.pollWebapp();

		// then
		http.assertSinglePostRequestMade();
		db.assertEmpty("wo_message");
	}

	@Test
	public void test_pollWebapp_shouldSaveMessagesFromResponseToDb() throws Exception {
		// given
		db.assertEmpty("wo_message");
		http.nextResponseJson("{ \"messages\": [ " +
					"{ \"id\": \"aaa-111\", \"to\": \"+1\", \"content\": \"testing: one\" }," +
					"{ \"id\": \"aaa-222\", \"to\": \"+2\", \"content\": \"testing: two\" }" +
				"] }");

		// when
		poller.pollWebapp();

		// then
		http.assertSinglePostRequestMade();
		db.assertTable("wo_message",
				"aaa-111", "UNSENT", false, ANY_NUMBER, "+1", "testing: one",
				"aaa-222", "UNSENT", false, ANY_NUMBER, "+2", "testing: two");
	}

	@Test
	public void test_pollWebapp_poorlyFormedWoMessagesShouldNotAffectWellFormed() throws Exception {
		// given
		db.assertEmpty("wo_message");
		http.nextResponseJson("{ \"messages\": [ " +
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
		http.assertSinglePostRequestMade();
		db.assertTable("wo_message",
				"ok-111", "UNSENT", false, ANY_NUMBER, "+1", "ok: one",
				"ok-222", "UNSENT", false, ANY_NUMBER, "+2", "ok: two");
	}
}
