package medic.gateway.alert;

import android.test.*;

import medic.gateway.alert.test.*;

import okhttp3.mockwebserver.*;

import org.json.*;
import org.junit.*;

import static org.junit.Assert.*;
import static medic.gateway.alert.test.DbTestHelper.*;
import static medic.gateway.alert.test.TestUtils.*;

@SuppressWarnings({"PMD.SignatureDeclareThrowsException", "PMD.JUnitTestsShouldIncludeAssert"})
public class WebappPollerTest extends AndroidTestCase {
	private static final String NO_REASON = null;

	private DbTestHelper db;
	private HttpTestHelper http;

	@Before
	public void setUp() throws Exception {
		super.setUp();

		db = new DbTestHelper(getContext());

		http = new HttpTestHelper();
		http.configureAppSettings(getContext());
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
				cols("_id",        "status",                   "last_action", "_from",        "content",    "sms_sent", "sms_received"),
				vals(randomUuid(), WtMessage.Status.FORWARDED, 0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals(waitingId,    WtMessage.Status.WAITING,   0,             A_PHONE_NUMBER, SOME_CONTENT, 1,          2),
				vals(randomUuid(), WtMessage.Status.FAILED,    0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0));
		http.nextResponseJson("{}");

		// when
		new WebappPoller(getContext()).pollWebapp();

		// then
		JSONObject requestBody = http.assertPostRequestMade_withJsonResponse();

		JSONArray messages = requestBody.getJSONArray("messages");
		assertEquals(1, messages.length());

		JSONObject message = (JSONObject) messages.get(0);
		assertEquals(waitingId, message.getString("id"));
		assertEquals(A_PHONE_NUMBER, message.getString("from"));
		assertEquals(SOME_CONTENT, message.getString("content"));
		assertEquals(1, message.getLong("sms_sent"));
		assertEquals(2, message.getLong("sms_received"));
	}

	@Test
	public void test_pollWebapp_shouldOnlyIncludeStatusUpdatesThatNeedForwarding() throws Exception {
		// given
		String deliveredId = randomUuid();
		String irrelevantId = randomUuid();
		db.insert("wo_message",
				cols("_id",        "status",                   "last_action", "_to",          "content", "retries"),
				vals(deliveredId,  WoMessage.Status.DELIVERED, 0,             A_PHONE_NUMBER, SOME_CONTENT, 0),
				vals(irrelevantId, WoMessage.Status.DELIVERED, 0,             A_PHONE_NUMBER, SOME_CONTENT, 0));
		db.insert("wom_status",
				cols("message_id", "status",                   "timestamp", "needs_forwarding"),
				vals(deliveredId,  WoMessage.Status.DELIVERED, 0,            true),
				vals(irrelevantId, WoMessage.Status.DELIVERED, 0,            false));
		http.nextResponseJson("{}");

		// when
		new WebappPoller(getContext()).pollWebapp();

		// then
		JSONObject requestBody = http.assertPostRequestMade_withJsonResponse();

		JSONArray updates = requestBody.getJSONArray("updates");
		assertEquals(1, updates.length());

		JSONObject update = (JSONObject) updates.get(0);
		assertEquals(deliveredId, update.getString("id"));
		assertEquals("DELIVERED", update.getString("status"));
	}

	@Test
	public void test_pollWebapp_shouldMarkForwardedStatusesAsSuchInDb() throws Exception {
		// given
		String messageId = randomUuid();
		db.insert("wo_message",
				cols("_id",     "status",                   "last_action", "_to",          "content", "retries"),
				vals(messageId, WoMessage.Status.DELIVERED, 0,             A_PHONE_NUMBER, SOME_CONTENT, 0));
		db.insert("wom_status",
				cols("message_id", "status",                   "timestamp", "needs_forwarding"),
				vals(messageId,    WoMessage.Status.DELIVERED, 0,           true));
		http.nextResponseJson("{}");

		// when
		new WebappPoller(getContext()).pollWebapp();

		// then
		http.assertPostRequestMade_withJsonResponse();
		// and
		db.assertTable("wo_message",
				ANY_ID, "DELIVERED", NO_REASON, 0, A_PHONE_NUMBER, SOME_CONTENT, 0);
		db.assertTable("wom_status",
				ANY_NUMBER, messageId, "DELIVERED", NO_REASON, 0, false);
	}

	@Test
	public void test_pollWebapp_shouldIncludeReasonForFailedDeliveries() throws Exception {
		// given
		String messageId = randomUuid();
		db.insert("wo_message",
				cols("_id",     "status",                "failure_reason",  "last_action", "_to",          "content", "retries"),
				vals(messageId, WoMessage.Status.FAILED, "something-awful", 0,             A_PHONE_NUMBER, SOME_CONTENT, 0));
		db.insert("wom_status",
				cols("message_id", "status",                "failure_reason",  "timestamp", "needs_forwarding"),
				vals(messageId,    WoMessage.Status.FAILED, "something-awful", 0,           true));
		http.nextResponseJson("{}");

		// when
		new WebappPoller(getContext()).pollWebapp();

		// then
		JSONObject response = http.assertPostRequestMade_withJsonResponse();
		JSONArray statusUpdates = response.getJSONArray("updates");
		assertEquals(1, statusUpdates.length());

		// and
		JSONObject update = statusUpdates.getJSONObject(0);
		assertEquals(messageId, update.getString("id"));
		assertEquals("FAILED", update.getString("status"));
		assertEquals("something-awful", update.getString("reason"));
	}

//> RESPONSE CONTENT TESTS
	@Test
	public void test_pollWebapp_shouldFailQuietlyIfResponseIsError() throws Exception {
		// given
		http.nextResponseError(500);

		// when
		new WebappPoller(getContext()).pollWebapp();

		// then
		http.assertSinglePostRequestMade();
		db.assertEmpty("wo_message");
		db.assertEmpty("wom_status");
	}

	@Test
	public void test_pollWebapp_shouldFailQuietlyIfResponseIsNotJson() throws Exception {
		// given
		http.nextResponseJson("muhahaha not really json! {}");

		// when
		new WebappPoller(getContext()).pollWebapp();

		// then
		http.assertSinglePostRequestMade();
		db.assertEmpty("wo_message");
		db.assertEmpty("wom_status");
	}

	@Test
	public void test_pollWebapp_shouldBeFineIfMessagesIsNotIncludedInResponse() throws Exception {
		// given
		http.nextResponseJson("{}");

		// when
		new WebappPoller(getContext()).pollWebapp();

		// then
		http.assertSinglePostRequestMade();
		db.assertEmpty("wo_message");
		db.assertEmpty("wom_status");
	}

	@Test
	public void test_pollWebapp_shouldBeFineIfMessagesIsNull() throws Exception {
		// given
		http.nextResponseJson("{ \"messages\":null }");

		// when
		new WebappPoller(getContext()).pollWebapp();

		// then
		http.assertSinglePostRequestMade();
		db.assertEmpty("wo_message");
		db.assertEmpty("wom_status");
	}

	@Test
	public void test_pollWebapp_shouldBeFineIfNoMessagesInResponse() throws Exception {
		// given
		http.nextResponseJson("{ \"messages\":[] }");

		// when
		new WebappPoller(getContext()).pollWebapp();

		// then
		http.assertSinglePostRequestMade();
		db.assertEmpty("wo_message");
		db.assertEmpty("wom_status");
	}

	@Test
	public void test_pollWebapp_shouldSaveMessagesFromResponseToDb() throws Exception {
		// given
		db.assertEmpty("wo_message");
		db.assertEmpty("wom_status");
		http.nextResponseJson("{ \"messages\": [ " +
					"{ \"id\": \"aaa-111\", \"to\": \"+1\", \"content\": \"testing: one\" }," +
					"{ \"id\": \"aaa-222\", \"to\": \"+2\", \"content\": \"testing: two\" }" +
				"] }");

		// when
		new WebappPoller(getContext()).pollWebapp();

		// then
		http.assertSinglePostRequestMade();
		db.assertTable("wo_message",
				"aaa-111", "UNSENT", NO_REASON, ANY_NUMBER, "+1", "testing: one", 0,
				"aaa-222", "UNSENT", NO_REASON, ANY_NUMBER, "+2", "testing: two", 0);
		db.assertTable("wom_status",
				ANY_NUMBER, "aaa-111", "UNSENT", NO_REASON, ANY_NUMBER, true,
				ANY_NUMBER, "aaa-222", "UNSENT", NO_REASON, ANY_NUMBER, true);
	}

	@Test
	public void test_pollWebapp_shouldStripSpecialCharactersInPhoneNumbersBeforeSavingToDb() throws Exception {
		// given
		db.assertEmpty("wo_message");
		db.assertEmpty("wom_status");
		http.nextResponseJson("{ \"messages\": [ " +
					"{ \"id\": \"abc-123\", \"to\": \"+1-2 3\", \"content\": \"testing: abc\" }" +
				"] }");

		// when
		new WebappPoller(getContext()).pollWebapp();

		// then
		http.assertSinglePostRequestMade();
		db.assertTable("wo_message",
				"abc-123", "UNSENT", NO_REASON, ANY_NUMBER, "+123", "testing: abc", 0);
		db.assertTable("wom_status",
				ANY_NUMBER, "abc-123", "UNSENT", NO_REASON, ANY_NUMBER, true);
	}

	@Test
	public void test_pollWebapp_poorlyFormedWoMessagesShouldNotAffectWellFormed() throws Exception {
		// given
		db.assertEmpty("wo_message");
		db.assertEmpty("wom_status");
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
		new WebappPoller(getContext()).pollWebapp();

		// then
		http.assertSinglePostRequestMade();
		db.assertTable("wo_message",
				"ok-111", "UNSENT", NO_REASON, ANY_NUMBER, "+1", "ok: one", 0,
				"ok-222", "UNSENT", NO_REASON, ANY_NUMBER, "+2", "ok: two", 0);
		db.assertTable("wom_status",
				ANY_NUMBER, "ok-111", "UNSENT", NO_REASON, ANY_NUMBER, true,
				ANY_NUMBER, "ok-222", "UNSENT", NO_REASON, ANY_NUMBER, true);
	}
}
