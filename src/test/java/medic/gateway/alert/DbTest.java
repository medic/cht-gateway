package medic.gateway.alert;

import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.telephony.*;

import java.lang.reflect.*;

import medic.gateway.alert.test.*;

import org.junit.*;
import org.junit.runner.*;
import org.robolectric.*;
import org.robolectric.annotation.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static medic.gateway.alert.test.DbTestHelper.*;
import static medic.gateway.alert.test.TestUtils.*;

@RunWith(RobolectricTestRunner.class)
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

	@Test
	public void test_deleteOldData_shouldHaveNoEffectIfNoData() {
		// given
		dbHelper.assertEmpty("log");
		dbHelper.assertEmpty("wo_message");
		dbHelper.assertEmpty("wt_message");

		// when
		int deletedCount = db.deleteOldData();

		// then
		assertEquals(0, deletedCount);
		dbHelper.assertEmpty("log");
		dbHelper.assertEmpty("wo_message");
		dbHelper.assertEmpty("wt_message");
	}

	@Test
	public void test_deleteOldData_shouldDeleteOldEventLogsButNotNewOnes() {
		// given
		dbHelper.insert("log",
				cols("_id", "timestamp", "message"),
				vals(1, now(), "should not delete 1"),
				vals(2, daysAgo(8), "should delete 1"),
				vals(3, daysAgo(6), "should not delete 2"),
				vals(4, daysAgo(800), "should delete 2"));
		assertEquals(4, dbHelper.count("log"));

		// when
		int deletedCount = db.deleteOldData();

		// then
		assertEquals(2, deletedCount);
		dbHelper.assertTable("log",
				ANY_ID, ANY_NUMBER, "should not delete 1",
				ANY_ID, ANY_NUMBER, "should not delete 2");
	}

	@Test
	public void test_deleteOldData_shouldDeleteOldWoMessagesButNotNewOnes() {
		// given
		dbHelper.insert("wo_message",
				cols("_id", "status", "status_needs_forwarding", "last_action", "_to", "content"),
				vals(randomUuid(), WoMessage.Status.PENDING, false, now(), A_PHONE_NUMBER, "should keep 1"),
				vals(randomUuid(), WoMessage.Status.PENDING, false, daysAgo(8), A_PHONE_NUMBER, "should delete 1"),
				vals(randomUuid(), WoMessage.Status.PENDING, false, daysAgo(6), A_PHONE_NUMBER, "should keep 2"),
				vals(randomUuid(), WoMessage.Status.PENDING, false, daysAgo(800), A_PHONE_NUMBER, "should delete 2"));
		assertEquals(4, dbHelper.count("wo_message"));

		// when
		int deletedCount = db.deleteOldData();

		// then
		assertEquals(2, deletedCount);
		dbHelper.assertTable("wo_message",
				ANY_ID, "PENDING", false, null, ANY_NUMBER, A_PHONE_NUMBER, "should keep 1",
				ANY_ID, "PENDING", false, null, ANY_NUMBER, A_PHONE_NUMBER, "should keep 2");
	}

	@Test
	public void test_deleteOldData_shouldDeleteOldWtMessagesButNotNewOnes() {
		// given
		dbHelper.insert("wt_message",
				cols("_id", "status", "last_action", "_from", "content"),
				vals(randomUuid(), WoMessage.Status.PENDING, now(), A_PHONE_NUMBER, "should keep 1"),
				vals(randomUuid(), WoMessage.Status.PENDING, daysAgo(8), A_PHONE_NUMBER, "should delete 1"),
				vals(randomUuid(), WoMessage.Status.PENDING, daysAgo(6), A_PHONE_NUMBER, "should keep 2"),
				vals(randomUuid(), WoMessage.Status.PENDING, daysAgo(800), A_PHONE_NUMBER, "should delete 2"));
		assertEquals(4, dbHelper.count("wt_message"));

		// when
		int deletedCount = db.deleteOldData();

		// then
		assertEquals(2, deletedCount);
		dbHelper.assertTable("wt_message",
				ANY_ID, "PENDING", ANY_NUMBER, A_PHONE_NUMBER, "should keep 1",
				ANY_ID, "PENDING", ANY_NUMBER, A_PHONE_NUMBER, "should keep 2");
	}

	@Test
	public void test_deleteOldData_shouldDeleteAllKindsOfData() {
		// given
		dbHelper.insert("log",
				cols("_id", "timestamp", "message"),
				vals(1, daysAgo(8), "Should be deleted"));
		dbHelper.insert("wo_message",
				cols("_id", "status", "status_needs_forwarding", "last_action", "_to", "content"),
				vals(randomUuid(), WoMessage.Status.PENDING, false, daysAgo(8), A_PHONE_NUMBER, "should delete"));
		dbHelper.insert("wt_message",
				cols("_id", "status", "last_action", "_from", "content"),
				vals(randomUuid(), WoMessage.Status.PENDING, daysAgo(8), A_PHONE_NUMBER, "should delete 1"));
		assertEquals(1, dbHelper.count("log"));
		assertEquals(1, dbHelper.count("wo_message"));
		assertEquals(1, dbHelper.count("wt_message"));

		// when
		int deletedCount = db.deleteOldData();

		// then
		assertEquals(3, deletedCount);
		dbHelper.assertEmpty("log");
		dbHelper.assertEmpty("wo_message");
		dbHelper.assertEmpty("wt_message");
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
