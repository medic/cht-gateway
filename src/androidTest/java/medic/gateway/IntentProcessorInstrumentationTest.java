package medic.gateway;

import android.content.*;
import android.database.*;
import android.test.*;

import java.lang.reflect.*;

import medic.gateway.test.*;

import org.junit.*;

import static android.app.Activity.RESULT_OK;
import static android.provider.Telephony.Sms.Intents.*;
import static android.telephony.SmsManager.RESULT_ERROR_GENERIC_FAILURE;
import static medic.gateway.WoMessage.Status.*;
import static medic.gateway.test.DbTestHelper.*;
import static medic.gateway.test.TestUtils.*;
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
		aDeliveryFailedReportArrivesFor(id);

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
		deliver(intentFor("medic.gateway.DELIVERY_REPORT", id));
	}

	private void aSendingReportArrivesFor(String id) {
		deliver(intentFor("medic.gateway.SENDING_REPORT", id), RESULT_OK);
	}

	private void aDeliveryFailedReportArrivesFor(String id) {
		deliver(intentFor("medic.gateway.SENDING_REPORT", id), RESULT_ERROR_GENERIC_FAILURE);
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
