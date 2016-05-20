package medic.gateway;

import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.telephony.*;

import java.lang.reflect.*;

import medic.gateway.test.*;

import org.junit.*;
import org.junit.runner.*;
import org.robolectric.*;
import org.robolectric.annotation.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static medic.gateway.test.DbTestHelper.*;
import static medic.gateway.test.TestUtils.*;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants=BuildConfig.class)
@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
public class DbTest {
	private Db db;

	private DbTestHelper dbHelper;

	@Before
	public void setUp() throws Exception {
		dbHelper = new DbTestHelper(RuntimeEnvironment.application);
		db = dbHelper.db;
	}

	@After
	public void tearDown() throws Exception {
		dbHelper.tearDown();
	}

	@Test
	public void test_classParamsShouldBeInitialised() {
		assertNotNull(db);
		assertNotNull(dbHelper);
	}

	@Test
	public void test_canStoreWtMessages() {
		// given
		dbHelper.assertEmpty("wt_message");
		WtMessage m = aMessageWith(WtMessage.Status.WAITING);

		// when
		boolean successReported = db.store(m);

		// then
		assertTrue(successReported);
		assertEquals(1, dbHelper.count("wt_message"));
	}

	@Test
	public void test_updateStatusFrom_shouldFailSilentlyIfMessageNotInDb() {
		// given
		WtMessage unsavedMessage = aMessageWith(WtMessage.Status.FORWARDED);

		// when
		db.updateStatusFrom(WtMessage.Status.WAITING, unsavedMessage);

		// then
		dbHelper.assertEmpty("wt_message");
	}

	@Test
	public void test_updateStatusFrom_shouldFailSilentlyIfWrongStatusFound() {
		// given
		String id = randomUuid();
		dbHelper.insert("wt_message",
				cols("_id", "status", "last_action", "_from", "content"),
				vals(id, WtMessage.Status.FORWARDED, 0, A_PHONE_NUMBER, SOME_CONTENT));
		WtMessage messageWithUpdatedStatus = aMessageWith(id, WtMessage.Status.FAILED);

		// when
		db.updateStatusFrom(WtMessage.Status.WAITING, messageWithUpdatedStatus);

		// then
		Cursor c = dbHelper.selectById("wt_message", cols("status", "last_action"), id);
		assertEquals("FORWARDED", c.getString(0));
		assertEquals(0, c.getLong(1));
	}

	@Test
	public void test_updateStatusFrom_shouldUpdateStatusOfMatchedMessageIfExpectedStatusFound() {
		// given
		String id = randomUuid();
		dbHelper.insert("wt_message",
				cols("_id", "status", "last_action", "_from", "content"),
				vals(id, WtMessage.Status.WAITING, 0, A_PHONE_NUMBER, SOME_CONTENT));
		WtMessage messageWithUpdatedStatus = aMessageWith(id, WtMessage.Status.FORWARDED);

		// when
		db.updateStatusFrom(WtMessage.Status.WAITING, messageWithUpdatedStatus);

		// then
		Cursor c = dbHelper.selectById("wt_message", cols("status", "last_action"), id);
		assertEquals("FORWARDED", c.getString(0));
		assertNotEquals(0, c.getLong(1));
	}

	@Test
	public void test_canStoreWoMessages() {
		// given
		dbHelper.assertEmpty("wo_message");
		WoMessage m = aMessageWith(WoMessage.Status.PENDING);

		// when
		boolean successReported = db.store(m);

		// then
		assertTrue(successReported);
		assertEquals(1, dbHelper.count("wo_message"));
	}

	@Test
	public void test_updateStatus_shouldFailSilentlyIfMessageNotInDb() {
		// given
		WoMessage unsavedMessage = aMessageWith(WoMessage.Status.PENDING);

		// when
		db.updateStatus(unsavedMessage, WoMessage.Status.PENDING, WoMessage.Status.DELIVERED);

		// then
		dbHelper.assertEmpty("wt_message");
	}

	@Test
	public void test_updateStatus_shouldFailSilentlyIfWrongStatusFound() {
		// given
		String id = randomUuid();
		dbHelper.insert("wo_message",
				cols("_id", "status", "status_needs_forwarding", "failure_reason", "last_action", "_to", "content"),
				vals(id, WoMessage.Status.FAILED, false, "failure-reason", 0, A_PHONE_NUMBER, SOME_CONTENT));
		WoMessage messageWithUpdatedStatus = aMessageWith(id, WoMessage.Status.PENDING);

		// when
		db.updateStatus(messageWithUpdatedStatus, WoMessage.Status.PENDING, WoMessage.Status.DELIVERED);

		// then
		Cursor c = dbHelper.selectById("wo_message", cols("status", "last_action"), id);
		assertEquals("FAILED", c.getString(0));
		assertEquals(0, c.getLong(1));
	}

	@Test
	public void test_updateStatus_shouldUpdateStatusOfMatchedMessageIfExpectedStatusFound() {
		// given
		String id = randomUuid();
		dbHelper.insert("wo_message",
				cols("_id", "status", "status_needs_forwarding", "last_action", "_to", "content"),
				vals(id, WoMessage.Status.PENDING, false, 0, A_PHONE_NUMBER, SOME_CONTENT));
		WoMessage messageWithUpdatedStatus = aMessageWith(id, WoMessage.Status.PENDING);

		// when
		db.updateStatus(messageWithUpdatedStatus, WoMessage.Status.PENDING, WoMessage.Status.DELIVERED);

		// then
		Cursor c = dbHelper.selectById("wo_message", cols("status", "last_action"), id);
		assertEquals("DELIVERED", c.getString(0));
		assertNotEquals(0, c.getLong(1));
	}

	@Test
	public void test_canStoreSmsMessages() {
		// given
		dbHelper.assertEmpty("wt_message");
		SmsMessage m = anSmsWith(A_PHONE_NUMBER, SOME_CONTENT);

		// when
		boolean successReported = db.store(m);

		// then
		assertTrue(successReported);
		dbHelper.assertTable("wt_message",
				ANY_ID, "WAITING", ANY_NUMBER, A_PHONE_NUMBER, SOME_CONTENT);
	}

//> STATIC HELPERS
	private static WtMessage aMessageWith(WtMessage.Status status) {
		return aMessageWith(randomUuid(), status);
	}

	private static WtMessage aMessageWith(String id, WtMessage.Status status) {
		return new WtMessage(id, status, System.currentTimeMillis(), A_PHONE_NUMBER, SOME_CONTENT);
	}

	private static WoMessage aMessageWith(WoMessage.Status status) {
		return aMessageWith(randomUuid(), status);
	}

	private static WoMessage aMessageWith(String id, WoMessage.Status status) {
		return new WoMessage(id, status, null, System.currentTimeMillis(), A_PHONE_NUMBER, SOME_CONTENT);
	}

	private static SmsMessage anSmsWith(String from, String content) {
		SmsMessage m = mock(SmsMessage.class);
		when(m.getMessageBody()).thenReturn(content);
		when(m.getOriginatingAddress()).thenReturn(from);
		return m;
	}
}
