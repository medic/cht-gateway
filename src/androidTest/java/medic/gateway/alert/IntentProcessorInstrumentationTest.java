package medic.gateway.alert;

import android.content.*;
import android.database.*;
import android.test.*;
import java.lang.reflect.*;
import medic.gateway.alert.test.*;
import org.junit.*;
import static android.app.Activity.RESULT_OK;
import static android.telephony.SmsManager.RESULT_ERROR_GENERIC_FAILURE;
import static android.telephony.SmsManager.RESULT_ERROR_NO_SERVICE;
import static android.telephony.SmsManager.RESULT_ERROR_NULL_PDU;
import static android.telephony.SmsManager.RESULT_ERROR_RADIO_OFF;
import static medic.gateway.alert.SmsCompatibility.SMS_DELIVER_ACTION;
import static medic.gateway.alert.WoMessage.Status.*;
import static medic.gateway.alert.test.DbTestHelper.*;
import static medic.gateway.alert.test.TestUtils.*;

@SuppressWarnings({"PMD", "PMD.SignatureDeclareThrowsException", "PMD.JUnitTestsShouldIncludeAssert"})
public class IntentProcessorInstrumentationTest extends AndroidTestCase {
	private IntentProcessor intentProcessor;

	private DbTestHelper db;
	private HttpTestHelper http;

	@Before
	public void setUp() throws Exception {
		super.setUp();

		this.db = new DbTestHelper(getContext());

		this.intentProcessor = new IntentProcessor();

		http = new HttpTestHelper();
		http.configureAppSettings(getContext());

		dummySend(true);
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();

		db.tearDown();
		http.tearDown();

		http.assertNoMoreRequests();

		dummySend(false);
	}

	public void dummySend(boolean dummySend) {
		SharedPreferences.Editor ed = getContext()
				.getSharedPreferences(SettingsStore.class.getName(), Context.MODE_PRIVATE)
				.edit();
		ed.putBoolean("dummy-send-enabled", dummySend);
		assertTrue(ed.commit());
	}

//> REQUEST CONTENT TESTS
	@Test
	public void test_onReceive_shouldUpdateSendStatusOfWoMessage() throws Exception {
		// given
		String id = randomUuid();
		aWoMessageIsInDbWith(id, PENDING, 0);

		// when
		aSendingReportArrivesFor(id);

		// then
		assertWoDbStatusOf(id, SENT);
	}

	@Test
	public void test_onReceive_GENERIC_shouldUpdateSendStatusAndIncludeErrorCodeInReason() throws Exception {
		// given
		String id = randomUuid();
		aWoMessageIsInDbWith(id, PENDING, 0);
		db.assertTable("wo_message",
				id, "PENDING", null, ANY_NUMBER, ANY_PHONE_NUMBER, ANY_CONTENT, 0);
		db.assertTable("wom_status",
				ANY_NUMBER, id, "PENDING", null, ANY_NUMBER, false);

		// when
		aSendFailureReportArrivesFor(id, RESULT_ERROR_GENERIC_FAILURE, 99);

		// then
		db.assertTable("wo_message",
				id, "FAILED", "generic; errorCode=99", ANY_NUMBER, ANY_PHONE_NUMBER, ANY_CONTENT, 0);
		db.assertTable("wom_status",
				ANY_NUMBER, id, "PENDING", null,                    ANY_NUMBER, false,
				ANY_NUMBER, id, "FAILED",  "generic; errorCode=99", ANY_NUMBER, true);
	}

	@Test
	public void test_onReceive_RADIO_OFF_shouldUpdateSendStatusAndIncludeErrorCodeInReason() throws Exception {
		// given
		String id = randomUuid();
		aWoMessageIsInDbWith(id, PENDING, 21); // Hard fail
		db.assertTable("wo_message",
				id, "PENDING", null, ANY_NUMBER, ANY_PHONE_NUMBER, ANY_CONTENT, 21);
		db.assertTable("wom_status",
				ANY_NUMBER, id, "PENDING", null, ANY_NUMBER, false);

		// when
		aSendFailureReportArrivesFor(id, RESULT_ERROR_RADIO_OFF);

		// then
		db.assertTable("wo_message",
				id, "FAILED", "radio-off", ANY_NUMBER, ANY_PHONE_NUMBER, ANY_CONTENT, 0);
		db.assertTable("wom_status",
				ANY_NUMBER, id, "PENDING", null,        ANY_NUMBER, false,
				ANY_NUMBER, id, "FAILED",  "radio-off", ANY_NUMBER, true);
	}

	@Test
	public void test_onReceive_RADIO_OFF_shouldUpdateSendStatusAndRetryAfterSoftFail() throws Exception {
		// given
		String id = randomUuid();
		aWoMessageIsInDbWith(id, PENDING, 0);
		db.assertTable("wo_message",
				id, "PENDING", null, ANY_NUMBER, ANY_PHONE_NUMBER, ANY_CONTENT, 0);
		db.assertTable("wom_status",
				ANY_NUMBER, id, "PENDING", null, ANY_NUMBER, false);

		// when
		aSendFailureReportArrivesFor(id, RESULT_ERROR_RADIO_OFF);

		// then
		db.assertTable("wo_message",
				id, "UNSENT", null, ANY_NUMBER, ANY_PHONE_NUMBER, ANY_CONTENT, 1);
		db.assertTable("wom_status",
				ANY_NUMBER, id, "PENDING", null,        ANY_NUMBER, false,
				ANY_NUMBER, id, "UNSENT",  null, ANY_NUMBER, true);
	}

	@Test
	public void test_onReceive_NO_SERVICE_shouldUpdateSendStatusAndIncludeErrorCodeInReason() throws Exception {
		// given
		String id = randomUuid();
		aWoMessageIsInDbWith(id, PENDING, 21); // Hard fail
		db.assertTable("wo_message",
				id, "PENDING", null, ANY_NUMBER, ANY_PHONE_NUMBER, ANY_CONTENT, 21);
		db.assertTable("wom_status",
				ANY_NUMBER, id, "PENDING", null, ANY_NUMBER, false);

		// when
		aSendFailureReportArrivesFor(id, RESULT_ERROR_NO_SERVICE);

		// then
		db.assertTable("wo_message",
				id, "FAILED", "no-service", ANY_NUMBER, ANY_PHONE_NUMBER, ANY_CONTENT, 0);
		db.assertTable("wom_status",
				ANY_NUMBER, id, "PENDING", null,         ANY_NUMBER, false,
				ANY_NUMBER, id, "FAILED",  "no-service", ANY_NUMBER, true);
	}

	@Test
	public void test_onReceive_NO_SERVICE_shouldUpdateSendStatusAndRetryAfterSoftFail() throws Exception {
		// given
		String id = randomUuid();
		aWoMessageIsInDbWith(id, PENDING, 0);
		db.assertTable("wo_message",
				id, "PENDING", null, ANY_NUMBER, ANY_PHONE_NUMBER, ANY_CONTENT, 0);
		db.assertTable("wom_status",
				ANY_NUMBER, id, "PENDING", null, ANY_NUMBER, false);

		// when
		aSendFailureReportArrivesFor(id, RESULT_ERROR_NO_SERVICE);

		// then
		db.assertTable("wo_message",
				id, "UNSENT", null, ANY_NUMBER, ANY_PHONE_NUMBER, ANY_CONTENT, 1);
		db.assertTable("wom_status",
				ANY_NUMBER, id, "PENDING", null,        ANY_NUMBER, false,
				ANY_NUMBER, id, "UNSENT",  null, ANY_NUMBER, true);
	}

	@Test
	public void test_onReceive_NULL_PDU_shouldUpdateSendStatusAndIncludeErrorCodeInReason() throws Exception {
		// given
		String id = randomUuid();
		aWoMessageIsInDbWith(id, PENDING, 21); // Hard fail
		db.assertTable("wo_message",
				id, "PENDING", null, ANY_NUMBER, ANY_PHONE_NUMBER, ANY_CONTENT, 21);
		db.assertTable("wom_status",
				ANY_NUMBER, id, "PENDING", null, ANY_NUMBER, false);

		// when
		aSendFailureReportArrivesFor(id, RESULT_ERROR_NULL_PDU);

		// then
		db.assertTable("wo_message",
				id, "FAILED", "null-pdu", ANY_NUMBER, ANY_PHONE_NUMBER, ANY_CONTENT, 0);
		db.assertTable("wom_status",
				ANY_NUMBER, id, "PENDING", null,       ANY_NUMBER, false,
				ANY_NUMBER, id, "FAILED",  "null-pdu", ANY_NUMBER, true);
	}

	@Test
	public void test_onReceive_NULL_PDU_shouldUpdateSendStatusAndRetryAfterSoftFail() throws Exception {
		// given
		String id = randomUuid();
		aWoMessageIsInDbWith(id, PENDING, 0);
		db.assertTable("wo_message",
				id, "PENDING", null, ANY_NUMBER, ANY_PHONE_NUMBER, ANY_CONTENT, 0);
		db.assertTable("wom_status",
				ANY_NUMBER, id, "PENDING", null, ANY_NUMBER, false);

		// when
		aSendFailureReportArrivesFor(id, RESULT_ERROR_NULL_PDU);

		// then
		db.assertTable("wo_message",
				id, "UNSENT", null, ANY_NUMBER, ANY_PHONE_NUMBER, ANY_CONTENT, 1);
		db.assertTable("wom_status",
				ANY_NUMBER, id, "PENDING", null,       ANY_NUMBER, false,
				ANY_NUMBER, id, "UNSENT",  null, ANY_NUMBER, true);
	}

	@Test
	public void test_onReceive_NULL_PDU_shouldContinueRetryAfterSoftFail() throws Exception {
		// given
		String id = randomUuid();
		aWoMessageIsInDbWith(id, PENDING, 10);
		db.assertTable("wo_message",
				id, "PENDING", null, ANY_NUMBER, ANY_PHONE_NUMBER, ANY_CONTENT, 10);
		db.assertTable("wom_status",
				ANY_NUMBER, id, "PENDING", null, ANY_NUMBER, false);

		// when
		aSendFailureReportArrivesFor(id, RESULT_ERROR_NULL_PDU);

		// then
		db.assertTable("wo_message",
				id, "UNSENT", null, ANY_NUMBER, ANY_PHONE_NUMBER, ANY_CONTENT, 11);
		db.assertTable("wom_status",
				ANY_NUMBER, id, "PENDING", null, ANY_NUMBER, false,
				ANY_NUMBER, id, "UNSENT",  null, ANY_NUMBER, true);
	}

	@Test
	public void test_onReceive_shouldUpdateDeliveryStatusOfSentWoMessage() throws Exception {
		// given
		String id = randomUuid();
		aWoMessageIsInDbWith(id, SENT, 0);

		// when
		aDeliveryReportArrivesFor(id);

		// then
		assertWoDbStatusOf(id, DELIVERED);
	}

	@Test
	public void test_onReceive_shouldUpdateDeliveryStatusOfPendingWoMessage() throws Exception {
		// given
		String id = randomUuid();
		aWoMessageIsInDbWith(id, PENDING, 0);

		// when
		aDeliveryReportArrivesFor(id);

		// then
		assertWoDbStatusOf(id, DELIVERED);
	}

	@Test
	public void test_onReceive_shouldNotUpdateStatusOfAlreadyDeliveredMessage() throws Exception {
		// given
		String id = randomUuid();
		aWoMessageIsInDbWith(id, DELIVERED, 0);

		// when
		aSendFailureReportArrivesFor(id);

		// then
		assertWoDbStatusOf(id, DELIVERED);
	}

	@Test
	public void test_onDeliver_shouldForwardToApiAndSendSMSResponses() throws Exception {
		// given
		// http.nextResponseJson("{}");
		http.nextResponseJson("{ \"messages\": [ " +
			"{ \"id\": \"aaa-111\", \"to\": \"+1\", \"content\": \"testing: one\" }," +
		"] }");


		// when
		aWtSmsArrives();

		// we need to wait for an async task to complete
		// this is hacky, but if we find we need it more we can formalise it
		int attemptsLeft = 10;
		while (true) {
			try {
				// we're using our last assertion to confirm the entire async flow has completed
				assertWoDbStatusOf("aaa-111", WoMessage.Status.DELIVERED);
				break;
			} catch (Error e) { // junit failures extend Error
				if (attemptsLeft == 0) {
					throw e;
				}

				Thread.sleep(100);
				attemptsLeft--;
			}
		}

		// then
		http.assertPostRequestMade_withJsonResponse();
		assertWtDbStatus(WtMessage.Status.FORWARDED);

		assertWoDbStatusOf("aaa-111", WoMessage.Status.DELIVERED);
	}

//> HELPER METHODS
	private void aWoMessageIsInDbWith(String id, WoMessage.Status status, int retries) {
		db.insert("wo_message",
				cols("_id", "status", "last_action", "_to", "content", "retries"),
				vals(id, status, 0, A_PHONE_NUMBER, SOME_CONTENT, retries));
		db.insert("wom_status",
				cols("message_id", "status", "timestamp", "needs_forwarding"),
				vals(id, status, 0, false));
	}

	private void assertWoDbStatusOf(String id, WoMessage.Status expectedStatus) {
		Cursor c = db.raw.rawQuery("SELECT status FROM wo_message WHERE _id=?", args(id));
		assertEquals(1, c.getCount());

		c.moveToFirst();
		assertEquals(expectedStatus.toString(), c.getString(0));

		c.close();
	}

	private void assertWtDbStatus(WtMessage.Status expectedStatus) {
		// Some in the expectedStatus
		Cursor c = db.raw.rawQuery("SELECT status FROM wt_message", NO_ARGS);

		c.moveToFirst();

		assertTrue(c.getCount() > 0);

		while (!c.isAfterLast()) {
			assertEquals(expectedStatus.toString(), c.getString(0));
			c.moveToNext();
		}

		c.close();
	}

	private void aDeliveryReportArrivesFor(String id) {
		Intent i = intentFor("medic.gateway.alert.DELIVERY_REPORT", id);
		i.putExtra("format", "3gpp");
		i.putExtra("pdu", A_VALID_DELIVERED_REPORT);
		deliver(i);
	}

	private void aSendingReportArrivesFor(String id) {
		deliver(intentFor("medic.gateway.alert.SENDING_REPORT", id), RESULT_OK);
	}

	private void aSendFailureReportArrivesFor(String id) {
		aSendFailureReportArrivesFor(id, RESULT_ERROR_GENERIC_FAILURE);
	}

	private void aSendFailureReportArrivesFor(String id, int resultCode) {
		deliver(intentFor("medic.gateway.alert.SENDING_REPORT", id), resultCode);
	}

	private void aSendFailureReportArrivesFor(String id, int resultCode, int errorCode) {
		Intent sendIntent = intentFor("medic.gateway.alert.SENDING_REPORT", id);
		sendIntent.putExtra("errorCode", errorCode);
		deliver(sendIntent, RESULT_ERROR_GENERIC_FAILURE);
	}

	private void aWtSmsArrives() {
		Intent i = new Intent(SMS_DELIVER_ACTION);
		i.putExtra("pdus", new Object[] {A_VALID_GSM_PDU});
		i.putExtra("format", "3gpp");
		deliver(i);
	}

	private Intent intentFor(String action, String id) {
		Intent i = new Intent(action);
		i.putExtra("id", id);
		return i;
	}

	private void deliver(Intent i, int resultCode) {
		try {
			Constructor c = BroadcastReceiver.PendingResult.class.getDeclaredConstructor(int.class, String.class, android.os.Bundle.class, int.class, boolean.class, boolean.class, android.os.IBinder.class, int.class);
			c.setAccessible(true);
			BroadcastReceiver.PendingResult pr = (BroadcastReceiver.PendingResult) c.newInstance(resultCode, null, null, 0, false, false, null, 0);

			Field f = BroadcastReceiver.class.getDeclaredField("mPendingResult");
			f.setAccessible(true);
			f.set(intentProcessor, pr);

			deliver(i);

			return;
		} catch(Exception ex) { /* ignore */ }

		try {
			Constructor c = BroadcastReceiver.PendingResult.class.getDeclaredConstructor(int.class, String.class, android.os.Bundle.class, int.class, boolean.class, boolean.class, android.os.IBinder.class, int.class, int.class);
			c.setAccessible(true);
			BroadcastReceiver.PendingResult pr = (BroadcastReceiver.PendingResult) c.newInstance(resultCode, null, null, 0, false, false, null, 0, 0);

			Field f = BroadcastReceiver.class.getDeclaredField("mPendingResult");
			f.setAccessible(true);
			f.set(intentProcessor, pr);

			deliver(i);

			return;
		} catch(Exception ex) { /* ignore */ }

		StringBuilder details = new StringBuilder();
		for(Constructor c : BroadcastReceiver.PendingResult.class.getDeclaredConstructors()) {
			details.append(c.getName());
			details.append('(');
			for(Class p : c.getParameterTypes()) {
				details.append(p.getName());
				details.append(',');
			}
			details.append("); ");
		}
		throw new RuntimeException("Looks like this version of Android isn't properly supported by this test :¬\\  " +
				"Maybe one of these contructors will help: " + details);

	}

	private void deliver(Intent i) {
		intentProcessor.onReceive(getContext(), i);
	}
}
