package medic.gateway.alert;

import android.test.AndroidTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import android.content.Intent;
import com.commonsware.cwac.wakeful.WakefulIntentService;

import medic.gateway.alert.test.DbTestHelper;
import medic.gateway.alert.test.HttpTestHelper;

import static medic.gateway.alert.test.DbTestHelper.cols;
import static medic.gateway.alert.test.DbTestHelper.vals;
import static medic.gateway.alert.test.TestUtils.ANY_NUMBER;
import static medic.gateway.alert.test.TestUtils.A_PHONE_NUMBER;
import static medic.gateway.alert.test.TestUtils.SOME_CONTENT;

@SuppressWarnings({"PMD.SignatureDeclareThrowsException", "PMD.JUnitTestsShouldIncludeAssert"})
public class WakefulServiceTest extends AndroidTestCase {
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

		// http.assertNoMoreRequests();

	}

	@Test
	public void test_doWakefulWork_unsentMessagesShouldSendMessages() throws Exception {
		// given
		db.insert("wt_message",
				cols("_id",        "status",                 "last_action", "_from",        "content",    "sms_sent", "sms_received"),
				vals("message-0001", WtMessage.Status.WAITING, 0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals("message-0002", WtMessage.Status.WAITING, 0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals("message-0003", WtMessage.Status.WAITING, 0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0));
		http.nextResponseJson("{}");

		// when
		Intent i = new Intent(getContext(), WakefulIntentService.class);
		WakefulService wfs = new WakefulService(getContext());
		wfs.doWakefulWork(i);

		//then
		db.assertTable("wt_message",
				"message-0001", "FORWARDED", ANY_NUMBER, A_PHONE_NUMBER, SOME_CONTENT, ANY_NUMBER, ANY_NUMBER,
				"message-0002", "FORWARDED", ANY_NUMBER, A_PHONE_NUMBER, SOME_CONTENT, ANY_NUMBER, ANY_NUMBER,
				"message-0003", "FORWARDED", ANY_NUMBER, A_PHONE_NUMBER, SOME_CONTENT, ANY_NUMBER, ANY_NUMBER);
	}

	@Test
	public void test_doWakefulWork_unsentMessagesShouldSendMultipleBatches() throws Exception {
		// given
		db.insert("wt_message",
				cols("_id",        "status",                 "last_action", "_from",        "content",    "sms_sent", "sms_received"),
				vals("message-1001", WtMessage.Status.WAITING, 0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals("message-1002", WtMessage.Status.WAITING, 0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals("message-1003", WtMessage.Status.WAITING, 0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals("message-1004", WtMessage.Status.WAITING, 0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals("message-1005", WtMessage.Status.WAITING, 0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals("message-1006", WtMessage.Status.WAITING, 0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals("message-1007", WtMessage.Status.WAITING, 0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals("message-1008", WtMessage.Status.WAITING, 0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals("message-1009", WtMessage.Status.WAITING, 0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals("message-1010", WtMessage.Status.WAITING, 0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals("message-1011", WtMessage.Status.WAITING, 0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals("message-1012", WtMessage.Status.WAITING, 0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals("message-1013", WtMessage.Status.WAITING, 0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals("message-1014", WtMessage.Status.WAITING, 0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals("message-1015", WtMessage.Status.WAITING, 0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals("message-1016", WtMessage.Status.WAITING, 0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0));
		http.nextResponseJson("{}");
		http.nextResponseJson("{}");

		// when
		Intent i = new Intent(getContext(), WakefulIntentService.class);
		WakefulService wfs = new WakefulService(getContext());
		wfs.doWakefulWork(i);

		//then
		db.assertTable("wt_message",
				"message-1001", "FORWARDED", ANY_NUMBER, A_PHONE_NUMBER, SOME_CONTENT, ANY_NUMBER, ANY_NUMBER,
				"message-1002", "FORWARDED", ANY_NUMBER, A_PHONE_NUMBER, SOME_CONTENT, ANY_NUMBER, ANY_NUMBER,
				"message-1003", "FORWARDED", ANY_NUMBER, A_PHONE_NUMBER, SOME_CONTENT, ANY_NUMBER, ANY_NUMBER,
				"message-1004", "FORWARDED", ANY_NUMBER, A_PHONE_NUMBER, SOME_CONTENT, ANY_NUMBER, ANY_NUMBER,
				"message-1005", "FORWARDED", ANY_NUMBER, A_PHONE_NUMBER, SOME_CONTENT, ANY_NUMBER, ANY_NUMBER,
				"message-1006", "FORWARDED", ANY_NUMBER, A_PHONE_NUMBER, SOME_CONTENT, ANY_NUMBER, ANY_NUMBER,
				"message-1007", "FORWARDED", ANY_NUMBER, A_PHONE_NUMBER, SOME_CONTENT, ANY_NUMBER, ANY_NUMBER,
				"message-1008", "FORWARDED", ANY_NUMBER, A_PHONE_NUMBER, SOME_CONTENT, ANY_NUMBER, ANY_NUMBER,
				"message-1009", "FORWARDED", ANY_NUMBER, A_PHONE_NUMBER, SOME_CONTENT, ANY_NUMBER, ANY_NUMBER,
				"message-1010", "FORWARDED", ANY_NUMBER, A_PHONE_NUMBER, SOME_CONTENT, ANY_NUMBER, ANY_NUMBER,
				"message-1011", "FORWARDED", ANY_NUMBER, A_PHONE_NUMBER, SOME_CONTENT, ANY_NUMBER, ANY_NUMBER,
				"message-1012", "FORWARDED", ANY_NUMBER, A_PHONE_NUMBER, SOME_CONTENT, ANY_NUMBER, ANY_NUMBER,
				"message-1013", "FORWARDED", ANY_NUMBER, A_PHONE_NUMBER, SOME_CONTENT, ANY_NUMBER, ANY_NUMBER,
				"message-1014", "FORWARDED", ANY_NUMBER, A_PHONE_NUMBER, SOME_CONTENT, ANY_NUMBER, ANY_NUMBER,
				"message-1015", "FORWARDED", ANY_NUMBER, A_PHONE_NUMBER, SOME_CONTENT, ANY_NUMBER, ANY_NUMBER,
				"message-1016", "FORWARDED", ANY_NUMBER, A_PHONE_NUMBER, SOME_CONTENT, ANY_NUMBER, ANY_NUMBER);
	}
}
