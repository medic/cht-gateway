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
import static medic.gateway.alert.WoMessage.Status.*;
import static medic.gateway.alert.test.DbTestHelper.*;
import static medic.gateway.alert.test.TestUtils.*;
import static org.junit.Assert.*;

@SuppressWarnings({"PMD", "PMD.SignatureDeclareThrowsException", "PMD.JUnitTestsShouldIncludeAssert"})
public class IntentProcessorInstrumentationTest extends AndroidTestCase {
	private IntentProcessor intentProcessor;

	private DbTestHelper db;

	@Before
	public void setUp() throws Exception {
		super.setUp();

		this.db = new DbTestHelper(getContext());

		this.intentProcessor = new IntentProcessor();
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();

		db.tearDown();
	}

//> REQUEST CONTENT TESTS
	@Test
	public void test_onReceive_shouldUpdateSendStatusOfWoMessage() throws Exception {
		// given
		String id = randomUuid();
		aWoMessageIsInDbWith(id, PENDING);

		// when
		aSendingReportArrivesFor(id);

		// then
		assertDbStatusOf(id, SENT);
	}

	@Test
	public void test_onReceive_GENERIC_shouldUdpateSendStatusAndIncludeErrorCodeInReason() throws Exception {
		// given
		String id = randomUuid();
		aWoMessageIsInDbWith(id, PENDING);
		db.assertTable("wo_message",
				id, "PENDING", ANY_NUMBER, null, false, ANY_PHONE_NUMBER, ANY_CONTENT);

		// when
		aSendFailureReportArrivesFor(id, RESULT_ERROR_GENERIC_FAILURE, 99);

		// then
		db.assertTable("wo_message",
				id, "FAILED", true, "generic:99", ANY_NUMBER, ANY_PHONE_NUMBER, ANY_CONTENT);
	}

	@Test
	public void test_onReceive_RADIO_OFF_shouldUdpateSendStatusAndIncludeErrorCodeInReason() throws Exception {
		// given
		String id = randomUuid();
		aWoMessageIsInDbWith(id, PENDING);
		db.assertTable("wo_message",
				id, "PENDING", ANY_NUMBER, null, false, ANY_PHONE_NUMBER, ANY_CONTENT);

		// when
		aSendFailureReportArrivesFor(id, RESULT_ERROR_RADIO_OFF);

		// then
		db.assertTable("wo_message",
				id, "FAILED", true, "radio-off", ANY_NUMBER, ANY_PHONE_NUMBER, ANY_CONTENT);
	}

	@Test
	public void test_onReceive_NO_SERVICE_shouldUdpateSendStatusAndIncludeErrorCodeInReason() throws Exception {
		// given
		String id = randomUuid();
		aWoMessageIsInDbWith(id, PENDING);
		db.assertTable("wo_message",
				id, "PENDING", ANY_NUMBER, null, false, ANY_PHONE_NUMBER, ANY_CONTENT);

		// when
		aSendFailureReportArrivesFor(id, RESULT_ERROR_NO_SERVICE);

		// then
		db.assertTable("wo_message",
				id, "FAILED", true, "no-service", ANY_NUMBER, ANY_PHONE_NUMBER, ANY_CONTENT);
	}

	@Test
	public void test_onReceive_NULL_PDU_shouldUdpateSendStatusAndIncludeErrorCodeInReason() throws Exception {
		// given
		String id = randomUuid();
		aWoMessageIsInDbWith(id, PENDING);
		db.assertTable("wo_message",
				id, "PENDING", ANY_NUMBER, null, false, ANY_PHONE_NUMBER, ANY_CONTENT);

		// when
		aSendFailureReportArrivesFor(id, RESULT_ERROR_NULL_PDU);

		// then
		db.assertTable("wo_message",
				id, "FAILED", true, "null-pdu", ANY_NUMBER, ANY_PHONE_NUMBER, ANY_CONTENT);
	}

	@Test
	public void test_onReceive_shouldUpdateDeliveryStatusOfSentWoMessage() throws Exception {
		// given
		String id = randomUuid();
		aWoMessageIsInDbWith(id, SENT);

		// when
		aDeliveryReportArrivesFor(id);

		// then
		assertDbStatusOf(id, DELIVERED);
	}

	@Test
	public void test_onReceive_shouldUpdateDeliveryStatusOfPendingWoMessage() throws Exception {
		// given
		String id = randomUuid();
		aWoMessageIsInDbWith(id, PENDING);

		// when
		aDeliveryReportArrivesFor(id);

		// then
		assertDbStatusOf(id, DELIVERED);
	}

	@Test
	public void test_onReceive_shouldNotUpdateStatusOfAlreadyDeliveredMessage() throws Exception {
		// given
		String id = randomUuid();
		aWoMessageIsInDbWith(id, DELIVERED);

		// when
		aSendFailureReportArrivesFor(id);

		// then
		assertDbStatusOf(id, DELIVERED);
	}

//> HELPER METHODS
	private void aWoMessageIsInDbWith(String id, WoMessage.Status status) {
		db.insert("wo_message",
				cols("_id", "status", "status_needs_forwarding", "last_action", "_to", "content"),
				vals(id, status, false, 0, A_PHONE_NUMBER, SOME_CONTENT));
	}

	private void assertDbStatusOf(String id, WoMessage.Status expectedStatus) {
		Cursor c = db.raw.rawQuery("SELECT COUNT(_id) FROM wo_message WHERE _id=? AND status=?",
				args(id, expectedStatus.toString()));
		assertEquals(1, c.getCount());

		c.moveToFirst();
		assertEquals(1, c.getLong(0));

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
		} catch(Exception ex) {
			throw new RuntimeException(ex);
		}

		deliver(i);
	}

	private void deliver(Intent i) {
		intentProcessor.onReceive(getContext(), i);
	}
}
