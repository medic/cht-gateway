package medic.gateway.alert;

import android.database.*;
import android.database.sqlite.*;
import android.telephony.*;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import medic.gateway.alert.test.*;
import org.junit.*;
import org.junit.runner.*;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static androidx.test.core.app.ApplicationProvider.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static medic.gateway.alert.test.DbTestHelper.*;
import static medic.gateway.alert.test.TestUtils.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=26)
@SuppressWarnings({
		"PMD.ExcessivePublicCount",
		"PMD.GodClass",
		"PMD.JUnitTestsShouldIncludeAssert",
		"PMD.SignatureDeclareThrowsException",
		"PMD.TooManyMethods",
		"PMD.ExcessiveClassLength"
})
public class DbTest {
	private Db db;

	private DbTestHelper dbHelper;

	@Before
	public void setUp() throws Exception {
		dbHelper = new DbTestHelper(getApplicationContext());
		db = dbHelper.getDb();

		db.setLogEntryLimit(50);
	}

	@After
	public void tearDown() throws Exception {
		if(dbHelper != null) dbHelper.tearDown();
	}

//> WtMessage TESTS
	@Test
	public void classParamsShouldBeInitialised() {
		assertNotNull(db);
		assertNotNull(dbHelper);
	}

	@Test
	public void canStoreWtMessages() {
		// given
		dbHelper.assertEmpty("wt_message");
		WtMessage m = aMessageWith(WtMessage.Status.WAITING);

		// when
		boolean successReported = db.store(m);

		// then
		assertTrue(successReported);
		dbHelper.assertCount("wt_message", 1);
	}

	@Test
	public void updateStatusFrom_shouldFailSilentlyIfMessageNotInDb() {
		// given
		WtMessage unsavedMessage = aMessageWith(WtMessage.Status.FORWARDED);

		// when
		db.updateStatusFrom(WtMessage.Status.WAITING, unsavedMessage);

		// then
		dbHelper.assertEmpty("wt_message");
		dbHelper.assertEmpty("wtm_status");
	}

	@Test
	public void updateStatusFrom_shouldFailSilentlyIfWrongStatusFound() {
		// given
		String id = randomUuid();
		dbHelper.insert("wt_message",
				cols("_id", "status",                   "last_action", "_from",        "content",    "sms_sent", "sms_received"),
				vals(id,    WtMessage.Status.FORWARDED, 0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0));
		WtMessage messageWithUpdatedStatus = aMessageWith(id, WtMessage.Status.FAILED);

		// when
		db.updateStatusFrom(WtMessage.Status.WAITING, messageWithUpdatedStatus);

		// then
		Cursor c = null;
		try {
			c = dbHelper.selectById("wt_message", cols("status", "last_action"), id);
			assertEquals("FORWARDED", c.getString(0));
			assertEquals(0, c.getLong(1));
		} catch (Exception e) {
			throw e;
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	@Test
	public void updateStatusFrom_shouldUpdateStatusOfMatchedMessageIfExpectedStatusFound() {
		// given
		String id = randomUuid();
		dbHelper.insert("wt_message",
				cols("_id", "status",                 "last_action", "_from",        "content",    "sms_sent", "sms_received"),
				vals(id,    WtMessage.Status.WAITING, 0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0));
		WtMessage messageWithUpdatedStatus = aMessageWith(id, WtMessage.Status.FORWARDED);

		// when
		db.updateStatusFrom(WtMessage.Status.WAITING, messageWithUpdatedStatus);

		// then
		Cursor c = null;
		try {
			c = dbHelper.selectById("wt_message", cols("status", "last_action"), id);
			assertEquals("FORWARDED", c.getString(0));
			assertNotEquals(0, c.getLong(1));
		} catch (Exception e) {
			throw e;
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

//> WtMessage.StatusUpdate TESTS
	@Test
	public void wtMessage_store_shouldCreateNewStatusUpdateTableEntry() {
		// given
		WtMessage m = aMessageWith(WtMessage.Status.WAITING);
		dbHelper.assertCount("wt_message", 0);
		dbHelper.assertCount("wtm_status", 0);

		// when
		db.store(m);

		// then
		dbHelper.assertTable("wtm_status",
				ANY_NUMBER, m.id, "WAITING", ANY_NUMBER);
	}

	@Test
	public void updateStatusFrom_shouldCreateNewStatusUpdateTableEntry() {
		// given
		String id = randomUuid();
		WtMessage m = aMessageWith(id, WtMessage.Status.WAITING);
		db.store(m);
		dbHelper.assertCount("wt_message", 1);
		dbHelper.assertCount("wtm_status", 1);

		// when
		db.updateStatusFrom(WtMessage.Status.WAITING,
				aMessageWith(id, WtMessage.Status.FORWARDED));

		// then
		dbHelper.assertTable("wtm_status",
				ANY_NUMBER, m.id, "WAITING",   ANY_NUMBER,
				ANY_NUMBER, m.id, "FORWARDED", ANY_NUMBER);
	}

	@Test
	public void wt_statusUpdates_shouldBeReturnedInDbOrderForTheRelevantMessage() {
		// given
		WtMessage m = aMessageWith("relevant", WtMessage.Status.FORWARDED);
		dbHelper.insert("wtm_status",
				cols("_id", "message_id", "status",    "timestamp"),
				vals(1,     "random1",    "WAITING",   111),
				vals(2,     "relevant",   "WAITING",   222),
				vals(3,     "random2",    "FAILED",    333),
				vals(4,     "relevant",   "FORWARDED", 444),
				vals(5,     "random3",    "FAILED",    555));

		// when
		List<WtMessage.StatusUpdate> updates = db.getStatusUpdates(m);

		// then
		assertListEquals(updates,
				new WtMessage.StatusUpdate(2, "relevant", WtMessage.Status.WAITING, 222),
				new WtMessage.StatusUpdate(4, "relevant", WtMessage.Status.FORWARDED, 444));
	}

//> SmsMessage TESTS
	@Test
	public void canStoreSmsMessages() {
		// given
		dbHelper.assertEmpty("wt_message");
		SmsMessage m = anSmsWith(A_PHONE_NUMBER, SOME_CONTENT);

		// when
		boolean successReported = db.store(m);

		// then
		assertTrue(successReported);
		dbHelper.assertTable("wt_message",
				ANY_ID, "WAITING", ANY_NUMBER, A_PHONE_NUMBER, SOME_CONTENT, ANY_NUMBER, ANY_NUMBER);
	}

//> Multipart SmsMessage TESTS
	@Test
	public void store_multipart_shouldStoreFirstPart() throws Exception {
		// given
		dbHelper.assertEmpty("wt_message");
		dbHelper.assertEmpty("wt_message_part");
		int randomReference = randomInt(256);
		SmsMessage m = aMultipartSmsWith(A_PHONE_NUMBER, SOME_CONTENT, randomReference, 1, 3);

		// when
		boolean successReported = db.store(m);

		// then
		assertTrue(successReported);
		dbHelper.assertEmpty("wt_message");
		dbHelper.assertTable("wt_message_part",
				A_PHONE_NUMBER, SOME_CONTENT, ANY_NUMBER, ANY_NUMBER, randomReference, 1, 3);
	}

	@Test
	public void store_multipart_shouldStoreSecondPart_alone() throws Exception {
		// given
		dbHelper.assertEmpty("wt_message");
		dbHelper.assertEmpty("wt_message_part");
		int randomReference = randomInt(256);
		SmsMessage m = aMultipartSmsWith(A_PHONE_NUMBER, SOME_CONTENT, randomReference, 2, 3);

		// when
		boolean successReported = db.store(m);

		// then
		assertTrue(successReported);
		dbHelper.assertEmpty("wt_message");
		dbHelper.assertTable("wt_message_part",
				A_PHONE_NUMBER, SOME_CONTENT, ANY_NUMBER, ANY_NUMBER, randomReference, 2, 3);
	}

	@Test
	public void store_multipart_shouldStoreThirdPart_alone() throws Exception {
		// given
		dbHelper.assertEmpty("wt_message");
		dbHelper.assertEmpty("wt_message_part");
		int randomReference = randomInt(256);
		SmsMessage m = aMultipartSmsWith(A_PHONE_NUMBER, SOME_CONTENT, randomReference, 3, 3);

		// when
		boolean successReported = db.store(m);

		// then
		assertTrue(successReported);
		dbHelper.assertEmpty("wt_message");
		dbHelper.assertTable("wt_message_part",
				A_PHONE_NUMBER, SOME_CONTENT, ANY_NUMBER, ANY_NUMBER, randomReference, 3, 3);
	}

	@Test
	public void store_multipart_shouldStoreSecondPart() throws Exception {
		// given
		dbHelper.assertEmpty("wt_message");
		dbHelper.assertEmpty("wt_message_part");
		int randomReference = randomInt(256);
		SmsMessage m1 = aMultipartSmsWith(A_PHONE_NUMBER, SOME_CONTENT, randomReference, 1, 3);
		SmsMessage m2 = aMultipartSmsWith(A_PHONE_NUMBER, SOME_CONTENT, randomReference, 2, 3);

		// when
		boolean successReported1 = db.store(m1);
		boolean successReported2 = db.store(m2);

		// then
		assertTrue(successReported1);
		assertTrue(successReported2);
		dbHelper.assertEmpty("wt_message");
		dbHelper.assertTable("wt_message_part",
				A_PHONE_NUMBER, SOME_CONTENT, ANY_NUMBER, ANY_NUMBER, randomReference, 1, 3,
				A_PHONE_NUMBER, SOME_CONTENT, ANY_NUMBER, ANY_NUMBER, randomReference, 2, 3);
	}

	@Test
	public void store_multipart_shouldStoreThirdPart() throws Exception {
		// given
		dbHelper.assertEmpty("wt_message");
		dbHelper.assertEmpty("wt_message_part");
		int randomReference = randomInt(256);
		SmsMessage m1 = aMultipartSmsWith(A_PHONE_NUMBER, "one ", randomReference, 1, 3);
		SmsMessage m2 = aMultipartSmsWith(A_PHONE_NUMBER, "two ", randomReference, 2, 3);
		SmsMessage m3 = aMultipartSmsWith(A_PHONE_NUMBER, "...3", randomReference, 3, 3);

		// when
		boolean successReported1 = db.store(m1);
		boolean successReported2 = db.store(m2);
		boolean successReported3 = db.store(m3);

		// then
		assertTrue(successReported1);
		assertTrue(successReported2);
		assertTrue(successReported3);
		dbHelper.assertTable("wt_message",
				ANY_ID, "WAITING", ANY_NUMBER, A_PHONE_NUMBER, "one two ...3", ANY_NUMBER, ANY_NUMBER);
		dbHelper.assertEmpty("wt_message_part");
	}

	@Test
	public void store_multipart_shouldProcessPartsInAnyOrder() throws Exception {
		// given
		dbHelper.assertEmpty("wt_message");
		dbHelper.assertEmpty("wt_message_part");
		int randomReference = randomInt(256);
		SmsMessage m1 = aMultipartSmsWith(A_PHONE_NUMBER, "one ", randomReference, 1, 3);
		SmsMessage m2 = aMultipartSmsWith(A_PHONE_NUMBER, "two ", randomReference, 2, 3);
		SmsMessage m3 = aMultipartSmsWith(A_PHONE_NUMBER, "...3", randomReference, 3, 3);

		// when
		boolean successReported1 = db.store(m2);
		boolean successReported2 = db.store(m3);
		boolean successReported3 = db.store(m1);

		// then
		assertTrue(successReported1);
		assertTrue(successReported2);
		assertTrue(successReported3);
		dbHelper.assertTable("wt_message",
				ANY_ID, "WAITING", ANY_NUMBER, A_PHONE_NUMBER, "one two ...3", ANY_NUMBER, ANY_NUMBER);
		dbHelper.assertEmpty("wt_message_part");
	}

	@Test
	public void store_multipart_shouldNotStoreSamePartMultipleTimes() throws Exception {
		// given
		dbHelper.assertEmpty("wt_message");
		dbHelper.assertEmpty("wt_message_part");
		int randomReference = randomInt(256);
		SmsMessage m_a = aMultipartSmsWith(A_PHONE_NUMBER, SOME_CONTENT, randomReference, 1, 3);
		SmsMessage m_b = aMultipartSmsWith(A_PHONE_NUMBER, SOME_CONTENT, randomReference, 1, 3);

		// when
		boolean successReported_a = db.store(m_a);
		boolean successReported_b = db.store(m_b);

		// then
		assertTrue(successReported_a);
		assertFalse(successReported_b);
		dbHelper.assertEmpty("wt_message");
		dbHelper.assertTable("wt_message_part",
				A_PHONE_NUMBER, SOME_CONTENT, ANY_NUMBER, ANY_NUMBER, randomReference, 1, 3);
	}

	@Test
	public void store_multipart_shouldNotStitchUnrelatedMessages() throws Exception {
		// given
		dbHelper.assertEmpty("wt_message");
		dbHelper.assertEmpty("wt_message_part");
		int randomReference = randomInt(256);
		SmsMessage m_a = aMultipartSmsWith(A_PHONE_NUMBER,       SOME_CONTENT, randomReference, 1, 2);
		SmsMessage m_b = aMultipartSmsWith(ANOTHER_PHONE_NUMBER, SOME_CONTENT, randomReference, 2, 2);

		// when
		boolean successReported_a = db.store(m_a);
		boolean successReported_b = db.store(m_b);

		// then
		assertTrue(successReported_a);
		assertTrue(successReported_b);
		dbHelper.assertEmpty("wt_message");
		dbHelper.assertTable("wt_message_part",
				A_PHONE_NUMBER,       SOME_CONTENT, ANY_NUMBER, ANY_NUMBER, randomReference, 1, 2,
				ANOTHER_PHONE_NUMBER, SOME_CONTENT, ANY_NUMBER, ANY_NUMBER, randomReference, 2, 2);
	}

	@Test
	public void store_multipart_shouldNotDeleteUnrelatedParts() throws Exception {
		// given
		dbHelper.assertEmpty("wt_message");
		dbHelper.assertEmpty("wt_message_part");
		int randomReference = randomInt(256);
		SmsMessage m_a1 = aMultipartSmsWith(A_PHONE_NUMBER,       "one ",       randomReference,   1, 2);
		SmsMessage m_a2 = aMultipartSmsWith(A_PHONE_NUMBER,       "two",        randomReference,   2, 2);
		SmsMessage m_b  = aMultipartSmsWith(ANOTHER_PHONE_NUMBER, SOME_CONTENT, randomReference,   2, 2);
		SmsMessage m_c  = aMultipartSmsWith(A_PHONE_NUMBER,       SOME_CONTENT, randomReference+1, 2, 2);

		// when
		boolean successReported_a1 = db.store(m_a1);
		boolean successReported_b  = db.store(m_b);
		boolean successReported_c  = db.store(m_c);
		boolean successReported_a2 = db.store(m_a2);

		// then
		assertTrue(successReported_a1);
		assertTrue(successReported_a2);
		assertTrue(successReported_b);
		assertTrue(successReported_c);
		dbHelper.assertTable("wt_message",
				ANY_ID, "WAITING", ANY_NUMBER, A_PHONE_NUMBER, "one two", ANY_NUMBER, ANY_NUMBER);
		dbHelper.assertTable("wt_message_part",
				ANOTHER_PHONE_NUMBER, SOME_CONTENT, ANY_NUMBER, ANY_NUMBER, randomReference,   2, 2,
				A_PHONE_NUMBER,       SOME_CONTENT, ANY_NUMBER, ANY_NUMBER, randomReference+1, 2, 2);
	}

//> WoMessage TESTS
	@Test
	public void canStoreWoMessages() {
		// given
		dbHelper.assertEmpty("wo_message");
		WoMessage m = aMessageWith(WoMessage.Status.PENDING);

		// when
		boolean successReported = db.store(m);

		// then
		assertTrue(successReported);
		dbHelper.assertCount("wo_message", 1);
	}

	@Test
	public void store_WoMessage_duplicate_shouldMarkAsNeedsForwarding() {
		// given: there is a message in the database which does NOT need forwarding
		String messageId = randomUuid();
		dbHelper.insert("wo_message",
				cols("_id",        "status",                 "failure_reason", "last_action", "_to",          "content", "retries"),
				vals(messageId,    WoMessage.Status.PENDING, null,             0,             A_PHONE_NUMBER, SOME_CONTENT, 0));
		dbHelper.insert("wom_status",
				cols("message_id", "status",                 "failure_reason", "timestamp", "needs_forwarding"),
				vals(messageId,    WoMessage.Status.PENDING, null,             0,           false));

		// when: try to store the same message again
		db.store(new WoMessage(messageId, A_PHONE_NUMBER, SOME_CONTENT));

		// then: none of the message details have changed except last action and needs forwarding
		dbHelper.assertTable("wo_message",
				messageId, "PENDING", null, GT_ZERO, A_PHONE_NUMBER, SOME_CONTENT, 0);
		// and: the message's status has been marked as "needs forwarding"
		dbHelper.assertTable("wom_status",
				ANY_NUMBER, messageId, "PENDING", null, 0, true);
	}

	@Test
	public void updateStatus_shouldFailSilentlyIfMessageNotInDb() {
		// given
		WoMessage unsavedMessage = aMessageWith(WoMessage.Status.PENDING);

		// when
		db.updateStatus(unsavedMessage, WoMessage.Status.PENDING, WoMessage.Status.DELIVERED);

		// then
		dbHelper.assertEmpty("wo_message");
		dbHelper.assertEmpty("wom_status");
	}

	@Test
	public void updateStatus_shouldFailSilentlyIfWrongStatusFound() {
		// given
		String id = randomUuid();
		dbHelper.insert("wo_message",
				cols("_id", "status", "failure_reason", "last_action", "_to", "content", "retries"),
				vals(id, WoMessage.Status.FAILED, "failure-reason", 0, A_PHONE_NUMBER, SOME_CONTENT, 0));
		dbHelper.insert("wom_status",
				cols("message_id", "status",                 "failure_reason", "timestamp", "needs_forwarding"),
				vals(id,           WoMessage.Status.FAILED,  null,             0,           false));
		WoMessage messageWithUpdatedStatus = aMessageWith(id, WoMessage.Status.PENDING);

		// when
		db.updateStatus(messageWithUpdatedStatus, WoMessage.Status.PENDING, WoMessage.Status.DELIVERED);

		// then
		Cursor c = null;
		try {
			c = dbHelper.selectById("wo_message", cols("status", "last_action"), id);
			assertEquals("FAILED", c.getString(0));
			assertEquals(0, c.getLong(1));
		} catch (Exception e) {
			throw e;
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	@Test
	public void updateStatus_shouldUpdateStatusOfMatchedMessageIfExpectedStatusFound() {
		// given
		String id = randomUuid();
		dbHelper.insert("wo_message",
				cols("_id", "status", "last_action", "_to", "content", "retries"),
				vals(id, WoMessage.Status.PENDING, 0, A_PHONE_NUMBER, SOME_CONTENT, 0));
		dbHelper.insert("wom_status",
				cols("message_id", "status",                 "failure_reason", "timestamp", "needs_forwarding"),
				vals(id,           WoMessage.Status.PENDING, null,             0,           false));
		WoMessage messageWithUpdatedStatus = aMessageWith(id, WoMessage.Status.PENDING);

		// when
		db.updateStatus(messageWithUpdatedStatus, WoMessage.Status.PENDING, WoMessage.Status.DELIVERED);

		// then
		Cursor c = null;
		try {
			c = dbHelper.selectById("wo_message", cols("status", "last_action"), id);
			assertEquals("DELIVERED", c.getString(0));
			assertNotEquals(0, c.getLong(1));
		} catch (Exception e) {
			throw e;
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

//> WoMessage.StatusUpdate TESTS
	@Test
	public void woMessage_store_shouldCreateNewStatusUpdateTableEntry() {
		// given
		WoMessage m = aMessageWith(WoMessage.Status.UNSENT);
		dbHelper.assertCount("wo_message", 0);
		dbHelper.assertCount("wom_status", 0);

		// when
		db.store(m);

		// then
		dbHelper.assertTable("wom_status",
				ANY_NUMBER, m.id, "UNSENT", null, ANY_NUMBER, true);
	}

	@Test
	public void updateStatus_shouldCreateNewStatusUpdateTableEntry() {
		// given
		WoMessage m = aMessageWith(WoMessage.Status.UNSENT);
		db.store(m);
		dbHelper.assertCount("wo_message", 1);
		dbHelper.assertCount("wom_status", 1);

		// when
		db.updateStatus(m, WoMessage.Status.PENDING);

		// then
		dbHelper.assertTable("wom_status",
				ANY_NUMBER, m.id, "UNSENT", null, ANY_NUMBER, true,
				ANY_NUMBER, m.id, "PENDING", null, ANY_NUMBER, true);
	}

	@Test
	public void wo_statusUpdates_shouldBeReturnedInDbOrderForTheRelevantMessage() {
		// given
		WoMessage m = aMessageWith("relevant", WoMessage.Status.SENT);
		dbHelper.insert("wom_status",
				cols("_id", "message_id", "status",  "timestamp", "needs_forwarding"),
				vals(1,     "random1",    "PENDING", 111,         true),
				vals(2,     "relevant",   "PENDING", 222,         true),
				vals(3,     "random2",    "UNSENT",  333,         true),
				vals(4,     "relevant",   "SENT",    444,         true),
				vals(5,     "random3",    "FAILED",  555,         true));

		// when
		List<WoMessage.StatusUpdate> updates = db.getStatusUpdates(m);

		// then
		assertListEquals(updates,
				new WoMessage.StatusUpdate(2, "relevant", WoMessage.Status.PENDING, null, 223),
				new WoMessage.StatusUpdate(4, "relevant", WoMessage.Status.SENT, null, 445));
	}

//> deleteOldData() TESTS
	@Test
	public void deleteOldData_shouldHaveNoEffectIfNoData() {
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
	public void deleteOldData_shouldDeleteOldEventLogsButNotNewOnes() {
		// given
		dbHelper.insert("log",
				cols("_id", "timestamp", "message"),
				vals(1, now(), "should not delete 1"),
				vals(2, daysAgo(8), "should delete 1"),
				vals(3, daysAgo(6), "should not delete 2"),
				vals(4, daysAgo(800), "should delete 2"));
		dbHelper.assertCount("log", 4);

		// when
		int deletedCount = db.deleteOldData();

		// then
		assertEquals(2, deletedCount);
		dbHelper.assertTable("log",
				ANY_ID, ANY_NUMBER, "should not delete 1",
				ANY_ID, ANY_NUMBER, "should not delete 2");
	}

	@Test
	public void deleteOldData_shouldDeleteOldWoMessagesButNotNewOnes() {
		// given
		dbHelper.insert("wo_message",
				cols("_id",        "status",                 "last_action", "_to",          "content", "retries"),
				vals(randomUuid(), WoMessage.Status.PENDING, now(),         A_PHONE_NUMBER, "should keep 1", 0),
				vals(randomUuid(), WoMessage.Status.PENDING, daysAgo(8),    A_PHONE_NUMBER, "should delete 1", 0),
				vals(randomUuid(), WoMessage.Status.PENDING, daysAgo(6),    A_PHONE_NUMBER, "should keep 2", 0),
				vals(randomUuid(), WoMessage.Status.PENDING, daysAgo(800),  A_PHONE_NUMBER, "should delete 2", 0));
		dbHelper.assertCount("wo_message", 4);

		// when
		int deletedCount = db.deleteOldData();

		// then
		assertEquals(2, deletedCount);
		dbHelper.assertTable("wo_message",
				ANY_ID, "PENDING", null, ANY_NUMBER, A_PHONE_NUMBER, "should keep 1", 0,
				ANY_ID, "PENDING", null, ANY_NUMBER, A_PHONE_NUMBER, "should keep 2", 0);
	}

	@Test
	public void deleteOldData_shouldDeleteOldWtMessagesButNotNewOnes() {
		// given
		dbHelper.insert("wt_message",
				cols("_id",        "status",                 "last_action", "_from",        "content",         "sms_sent",    "sms_received"),
				vals(randomUuid(), WoMessage.Status.PENDING, now(),         A_PHONE_NUMBER, "should keep 1",   0,             0),
				vals(randomUuid(), WoMessage.Status.PENDING, daysAgo(8),    A_PHONE_NUMBER, "should delete 1", 0,             0),
				vals(randomUuid(), WoMessage.Status.PENDING, daysAgo(6),    A_PHONE_NUMBER, "should keep 2",   0,             0),
				vals(randomUuid(), WoMessage.Status.PENDING, daysAgo(800),  A_PHONE_NUMBER, "should delete 2", 0,             0));
		dbHelper.assertCount("wt_message", 4);

		// when
		int deletedCount = db.deleteOldData();

		// then
		assertEquals(2, deletedCount);
		dbHelper.assertTable("wt_message",
				ANY_ID, "PENDING", ANY_NUMBER, A_PHONE_NUMBER, "should keep 1", ANY_NUMBER, ANY_NUMBER,
				ANY_ID, "PENDING", ANY_NUMBER, A_PHONE_NUMBER, "should keep 2", ANY_NUMBER, ANY_NUMBER);
	}

	@Test
	public void deleteOldData_shouldDeleteAllKindsOfData() {
		// given
		dbHelper.insert("log",
				cols("_id", "timestamp", "message"),
				vals(1,     daysAgo(8),  "Should be deleted"));
		dbHelper.insert("wo_message",
				cols("_id",        "status",                 "last_action", "_to",          "content", "retries"),
				vals(randomUuid(), WoMessage.Status.PENDING, daysAgo(8),    A_PHONE_NUMBER, "should delete", 0));
		dbHelper.insert("wt_message",
				cols("_id",        "status",                 "last_action", "_from",        "content",         "sms_sent", "sms_received"),
				vals(randomUuid(), WoMessage.Status.PENDING, daysAgo(8),    A_PHONE_NUMBER, "should delete 1", 0,          0));
		dbHelper.assertCount("log", 1);
		dbHelper.assertCount("wo_message", 1);
		dbHelper.assertCount("wt_message", 1);

		// when
		int deletedCount = db.deleteOldData();

		// then
		assertEquals(3, deletedCount);
		dbHelper.assertEmpty("log");
		dbHelper.assertEmpty("wo_message");
		dbHelper.assertEmpty("wt_message");
	}

//> cleanLogs() TESTS
	@Test
	public void cleanLogs_shouldNotComplainIfNoLogs() {
		// given: nothing in the db

		// when:
		db.cleanLogs();

		// then
		dbHelper.assertEmpty("log");
	}

	@Test
	public void cleanLogs_shouldNotDeleteAnythingIfHardlyAnyLogs() {
		// given:
		for(int i=0; i<3; ++i) dbHelper.insert("log", cols("timestamp", "message"), vals(now(), "entry: " + i));
		dbHelper.assertCount("log", 3);

		// when:
		db.cleanLogs();

		// then
		dbHelper.assertCount("log", 3);
	}

	@Test
	public void cleanLogs_shouldNotDeleteAnythingIfNotEnoughLogs() {
		// given:
		for(int i=0; i<50; ++i) dbHelper.insert("log", cols("timestamp", "message"), vals(now(), "entry: " + i));
		dbHelper.assertCount("log", 50);

		// when:
		db.cleanLogs();

		// then
		dbHelper.assertCount("log", 50);
	}

	@Test
	public void cleanLogs_shouldOnlyDeleteOldestEntriesOverTheLimit() {
		// given
		for(int i=0; i<100; ++i) dbHelper.insert("log", cols("timestamp", "message"), vals(now(), "entry: " + i));
		dbHelper.assertCount("log", 100);

		// when:
		db.cleanLogs();

		// then
		dbHelper.assertCount("log", 50);
		dbHelper.assertValues("log", cols("message"),
				"entry: 50", "entry: 51", "entry: 52", "entry: 53", "entry: 54",
				"entry: 55", "entry: 56", "entry: 57", "entry: 58", "entry: 59",
				"entry: 60", "entry: 61", "entry: 62", "entry: 63", "entry: 64",
				"entry: 65", "entry: 66", "entry: 67", "entry: 68", "entry: 69",
				"entry: 70", "entry: 71", "entry: 72", "entry: 73", "entry: 74",
				"entry: 75", "entry: 76", "entry: 77", "entry: 78", "entry: 79",
				"entry: 80", "entry: 81", "entry: 82", "entry: 83", "entry: 84",
				"entry: 85", "entry: 86", "entry: 87", "entry: 88", "entry: 89",
				"entry: 90", "entry: 91", "entry: 92", "entry: 93", "entry: 94",
				"entry: 95", "entry: 96", "entry: 97", "entry: 98", "entry: 99");
	}

	@Test
	public void cleanLogs_shouldOnlyDeleteOldestEntriesOverTheLimit_withMoreMessages() {
		// given
		for(int i=0; i<500; ++i) dbHelper.insert("log", cols("timestamp", "message"), vals(now(), "entry: " + i));
		dbHelper.assertCount("log", 500);

		// when:
		db.cleanLogs();

		// then
		dbHelper.assertCount("log", 50);
		dbHelper.assertValues("log", cols("message"),
				"entry: 450", "entry: 451", "entry: 452", "entry: 453", "entry: 454",
				"entry: 455", "entry: 456", "entry: 457", "entry: 458", "entry: 459",
				"entry: 460", "entry: 461", "entry: 462", "entry: 463", "entry: 464",
				"entry: 465", "entry: 466", "entry: 467", "entry: 468", "entry: 469",
				"entry: 470", "entry: 471", "entry: 472", "entry: 473", "entry: 474",
				"entry: 475", "entry: 476", "entry: 477", "entry: 478", "entry: 479",
				"entry: 480", "entry: 481", "entry: 482", "entry: 483", "entry: 484",
				"entry: 485", "entry: 486", "entry: 487", "entry: 488", "entry: 489",
				"entry: 490", "entry: 491", "entry: 492", "entry: 493", "entry: 494",
				"entry: 495", "entry: 496", "entry: 497", "entry: 498", "entry: 499");
	}

//> MESSAGE REPORT TESTS
	@Test
	public void generateMessageReport_forAnEmptyDatabase_shouldGiveZeroForEverything() {
		// when
		MessageReport r = db.generateMessageReport();

		// then
		assertEquals(0, r.womCount);
		for(WoMessage.Status s : WoMessage.Status.values())
			assertEquals(0, r.getCount(s));

		assertEquals(0, r.wtmCount);
		for(WtMessage.Status s : WtMessage.Status.values())
			assertEquals(0, r.getCount(s));
	}

	@Test
	public void generateMessageReport_shouldCountThingsReliably() {
		// given
		dbHelper.insert("wo_message",
				cols("_id",        "status",                  "last_action", "_to",          "content", "retries"),
				vals(randomUuid(), WoMessage.Status.UNSENT,   0,             A_PHONE_NUMBER, "", 0),
				vals(randomUuid(), WoMessage.Status.PENDING,  0,             A_PHONE_NUMBER, "", 0),
				vals(randomUuid(), WoMessage.Status.PENDING,  0,             A_PHONE_NUMBER, "", 0),
				vals(randomUuid(), WoMessage.Status.SENT,     0,             A_PHONE_NUMBER, "", 0),
				vals(randomUuid(), WoMessage.Status.SENT,     0,             A_PHONE_NUMBER, "", 0),
				vals(randomUuid(), WoMessage.Status.SENT,     0,             A_PHONE_NUMBER, "", 0),
				vals(randomUuid(), WoMessage.Status.FAILED,   0,             A_PHONE_NUMBER, "", 0),
				vals(randomUuid(), WoMessage.Status.FAILED,   0,             A_PHONE_NUMBER, "", 0),
				vals(randomUuid(), WoMessage.Status.FAILED,   0,             A_PHONE_NUMBER, "", 0),
				vals(randomUuid(), WoMessage.Status.FAILED,   0,             A_PHONE_NUMBER, "", 0),
				vals(randomUuid(), WoMessage.Status.DELIVERED,0,             A_PHONE_NUMBER, "", 0),
				vals(randomUuid(), WoMessage.Status.DELIVERED,0,             A_PHONE_NUMBER, "", 0),
				vals(randomUuid(), WoMessage.Status.DELIVERED,0,             A_PHONE_NUMBER, "", 0),
				vals(randomUuid(), WoMessage.Status.DELIVERED,0,             A_PHONE_NUMBER, "", 0),
				vals(randomUuid(), WoMessage.Status.DELIVERED,0,             A_PHONE_NUMBER, "", 0));
		dbHelper.insert("wt_message",
				cols("_id",        "status",                   "last_action", "_from",        "content",    "sms_sent", "sms_received"),
				vals(randomUuid(), WtMessage.Status.WAITING,   0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals(randomUuid(), WtMessage.Status.WAITING,   0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals(randomUuid(), WtMessage.Status.WAITING,   0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals(randomUuid(), WtMessage.Status.WAITING,   0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals(randomUuid(), WtMessage.Status.WAITING,   0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals(randomUuid(), WtMessage.Status.WAITING,   0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals(randomUuid(), WtMessage.Status.FORWARDED, 0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals(randomUuid(), WtMessage.Status.FORWARDED, 0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals(randomUuid(), WtMessage.Status.FORWARDED, 0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals(randomUuid(), WtMessage.Status.FORWARDED, 0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals(randomUuid(), WtMessage.Status.FORWARDED, 0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals(randomUuid(), WtMessage.Status.FORWARDED, 0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals(randomUuid(), WtMessage.Status.FORWARDED, 0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals(randomUuid(), WtMessage.Status.FAILED,    0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals(randomUuid(), WtMessage.Status.FAILED,    0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals(randomUuid(), WtMessage.Status.FAILED,    0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals(randomUuid(), WtMessage.Status.FAILED,    0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals(randomUuid(), WtMessage.Status.FAILED,    0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals(randomUuid(), WtMessage.Status.FAILED,    0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals(randomUuid(), WtMessage.Status.FAILED,    0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0),
				vals(randomUuid(), WtMessage.Status.FAILED,    0,             A_PHONE_NUMBER, SOME_CONTENT, 0,          0));

		// when
		MessageReport r = db.generateMessageReport();

		// then
		assertEquals(15, r.womCount);
		assertEquals(1, r.getCount(WoMessage.Status.UNSENT));
		assertEquals(2, r.getCount(WoMessage.Status.PENDING));
		assertEquals(3, r.getCount(WoMessage.Status.SENT));
		assertEquals(4, r.getCount(WoMessage.Status.FAILED));
		assertEquals(5, r.getCount(WoMessage.Status.DELIVERED));

		assertEquals(21, r.wtmCount);
		assertEquals(6, r.getCount(WtMessage.Status.WAITING));
		assertEquals(7, r.getCount(WtMessage.Status.FORWARDED));
		assertEquals(8, r.getCount(WtMessage.Status.FAILED));
	}

//> MIGRATION TESTS
	@Test
	public void migrate_addRetriesColumn_WoMessage_ShouldAddColumn() {
		// given: some messages exist
		DbTestHelper dbHelper = anEmptyDbHelper();
		dbHelper.raw.execSQL("CREATE TABLE wo_message (" +
				"'_id' TEXT NOT NULL PRIMARY KEY, " +
				"'status' TEXT NOT NULL, " +
				"'failure_reason' TEXT, " +
				"'last_action' INTEGER NOT NULL, " +
				"'_to' TEXT NOT NULL, " +
				"'content' TEXT NOT NULL)");

		dbHelper.insert("wo_message",
				cols("_id", "status", "failure_reason", "last_action", "_to", "content"),
				vals("m-1", WoMessage.Status.UNSENT, null, 0, A_PHONE_NUMBER, ""),
				vals("m-2", WoMessage.Status.PENDING, null, 0, A_PHONE_NUMBER, ""),
				vals("m-3", WoMessage.Status.SENT, null, 0, A_PHONE_NUMBER, ""));

		// when
		Db.migrate_addRetriesColumn_WoMessage(dbHelper.raw);

		// then
		dbHelper.assertTable("wo_message",
				"m-1", WoMessage.Status.UNSENT, null, ANY_NUMBER, ANY_PHONE_NUMBER, "", 0,
				"m-2", WoMessage.Status.PENDING, null, ANY_NUMBER, ANY_PHONE_NUMBER, "", 0,
				"m-3", WoMessage.Status.SENT, null, ANY_NUMBER, ANY_PHONE_NUMBER, "", 0);
	}

	@Test
	public void migrate_createTable_WoMessageStatusUpdate_shouldCreateStatusesFromTheWoMessageTable() {
		// given: some messages exist
		DbTestHelper dbHelper = anEmptyDbHelper();
		dbHelper.raw.execSQL("CREATE TABLE wo_message (" +
				"'_id' TEXT NOT NULL, " +
				"'status' TEXT NOT NULL, " +
				"'failure_reason' TEXT, " +
				"'last_action' INTEGER NOT NULL, " +
				"'status_needs_forwarding' INTEGER NOT NULL)");
		dbHelper.insert("wo_message",
				cols("_id", "status",                   "failure_reason", "last_action", "status_needs_forwarding"),
				vals("a-1", WoMessage.Status.UNSENT,    null,              1,            0),
				vals("b-2", WoMessage.Status.PENDING,   null,              2,            1),
				vals("c-3", WoMessage.Status.SENT,      null,              3,            0),
				vals("d-4", WoMessage.Status.FAILED,    "some-reason",     4,            1),
				vals("e-5", WoMessage.Status.DELIVERED, null,              5,            0));

		// when
		Db.migrate_createTable_WoMessageStatusUpdate(dbHelper.raw, false);

		// then
		dbHelper.assertTable("wom_status",
				ANY_NUMBER, "a-1", "UNSENT",    null,          1, 0,
				ANY_NUMBER, "b-2", "PENDING",   null,          2, 1,
				ANY_NUMBER, "c-3", "SENT",      null,          3, 0,
				ANY_NUMBER, "d-4", "FAILED",    "some-reason", 4, 1,
				ANY_NUMBER, "e-5", "DELIVERED", null,          5, 0);
	}

	@Test
	public void migrate_createTable_WtMessageStatusUpdate_shouldCreateStatusesFromTheWtMessageTable() {
		// given: some messages exist
		DbTestHelper dbHelper = anEmptyDbHelper();
		dbHelper.raw.execSQL("CREATE TABLE wt_message (" +
				"'_id' TEXT NOT NULL, " +
				"'status' TEXT NOT NULL, " +
				"'last_action' INTEGER NOT NULL)");
		dbHelper.insert("wt_message",
				cols("_id", "status",                   "last_action"),
				vals("a-1", WtMessage.Status.WAITING,   1),
				vals("b-2", WtMessage.Status.FORWARDED, 2),
				vals("c-3", WtMessage.Status.FAILED,    3));

		// when
		Db.migrate_createTable_WtMessageStatusUpdate(dbHelper.raw, false);

		// then
		dbHelper.assertTable("wtm_status",
				ANY_NUMBER, "a-1", "WAITING",   1,
				ANY_NUMBER, "b-2", "FORWARDED", 2,
				ANY_NUMBER, "c-3", "FAILED",    3);
	}

	@Test
	public void migrate_create_WOS_clmNEEDS_FORWARDING() {
		// given
		DbTestHelper dbHelper = anEmptyDbHelper();
		dbHelper.raw.execSQL("CREATE TABLE wo_message (" +
				"'_id' TEXT NOT NULL, " +
				"'status' TEXT NOT NULL, " +
				"'failure_reason' TEXT, " +
				"'last_action' INTEGER NOT NULL, " +
				"'status_needs_forwarding' INTEGER NOT NULL)");
		dbHelper.raw.execSQL("CREATE TABLE wom_status (" +
				"'message_id' TEXT NOT NULL, " +
				"'status' TEXT NOT NULL, " +
				"'failure_reason' TEXT, " +
				"'timestamp' INTEGER NOT NULL)");
		dbHelper.insert("wo_message",
				cols("_id", "status",                 "last_action", "status_needs_forwarding"),
				vals("a-1", WoMessage.Status.UNSENT,   1,            0),
				vals("b-2", WoMessage.Status.PENDING,  2,            1),
				vals("c-3", WoMessage.Status.SENT,     3,            1),
				vals("d-4", WoMessage.Status.SENT,     4,            1));
		dbHelper.insert("wom_status",
				cols("message_id", "status",                   "timestamp"),
				vals("a-1",         WoMessage.Status.UNSENT,   1),
				vals("b-2",         WoMessage.Status.UNSENT,   1),
				vals("b-2",         WoMessage.Status.PENDING,  2),
				vals("c-3",         WoMessage.Status.UNSENT,   1),
				vals("c-3",         WoMessage.Status.PENDING,  2),
				vals("c-3",         WoMessage.Status.SENT,     3),
				vals("d-4",         WoMessage.Status.SENT,     1));

		// when
		Db.migrate_create_WOS_clmNEEDS_FORWARDING(dbHelper.raw);

		// then
		dbHelper.assertTable("wom_status",
				"a-1", "UNSENT",  null, 1, 0,
				"b-2", "UNSENT",  null, 1, 0,
				"b-2", "PENDING", null, 2, 1,
				"c-3", "UNSENT",  null, 1, 0,
				"c-3", "PENDING", null, 2, 0,
				"c-3", "SENT",    null, 3, 1,
				"d-4", "SENT",    null, 1, 0);
	}

	@Test
	public void migrate_create_WTM_clmSMS_SENT__clmSMS_RECEIVED() {
		// given
		DbTestHelper dbHelper = anEmptyDbHelper();
		dbHelper.raw.execSQL("CREATE TABLE wt_message (" +
				"'_id' TEXT NOT NULL)");
		dbHelper.insert("wt_message",
				cols("_id"),
				vals("a-1"));

		// when
		Db.migrate_create_WTM_clmSMS_SENT__clmSMS_RECEIVED(dbHelper.raw);

		// then
		dbHelper.assertTable("wt_message",
				"a-1", 0, 0);
	}

//> STATIC HELPERS
	private static WtMessage aMessageWith(WtMessage.Status status) {
		return aMessageWith(randomUuid(), status);
	}

	private static WtMessage aMessageWith(String id, WtMessage.Status status) {
		long timestamp = System.currentTimeMillis();
		return new WtMessage(id, status, timestamp, A_PHONE_NUMBER, SOME_CONTENT, timestamp-2, timestamp-1);
	}

	private static WoMessage aMessageWith(WoMessage.Status status) {
		return aMessageWith(randomUuid(), status);
	}

	private static WoMessage aMessageWith(String id, WoMessage.Status status) {
		return new WoMessage(id, status, null, System.currentTimeMillis(), A_PHONE_NUMBER, SOME_CONTENT, 0);
	}

	private static SmsMessage anSmsWith(String from, String content) {
		SmsMessage m = mock(SmsMessage.class);
		when(m.getMessageBody()).thenReturn(content);
		when(m.getOriginatingAddress()).thenReturn(from);
		when(m.getPdu()).thenReturn(null);
		return m;
	}

	private static SmsMessage aMultipartSmsWith(String from, String content, int referenceNumber, int partNumber, int totalParts) throws IOException {
		SmsMessage m = mock(SmsMessage.class);
		when(m.getMessageBody()).thenReturn(content);
		when(m.getOriginatingAddress()).thenReturn(from);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		UDH: {
			DataOutputStream pdu = new DataOutputStream(baos);

			pdu.write(0);               // empty SMSC number
			pdu.write(1 << 6);          // byte0, including UDH-present flag
			pdu.write(0); pdu.write(0); // an empty FROM number, but min length is 1
			pdu.write(0); pdu.write(0); // PID and DCS (both ignored in our SmsUdh)
			pdu.write(0); pdu.write(0); // \
			pdu.write(0); pdu.write(0); // |- timestamp (ignored)
			pdu.write(0); pdu.write(0); // |
			pdu.write(0);               // /
			pdu.write(0);               // UD Length byte (ignored in our SmsUdh
			pdu.write(5);               // number of bytes following this one...
			pdu.write(0);               // element type id: 8-bit concat info
			pdu.write(5);               // element content length (including this and the previous byte)
			pdu.write(referenceNumber);
			pdu.write(totalParts);
			pdu.write(partNumber);
		}
		when(m.getPdu()).thenReturn(baos.toByteArray());

		return m;
	}

	private static DbTestHelper anEmptyDbHelper() {
		@SuppressWarnings("PMD.UncommentedEmptyMethodBody")
		SQLiteOpenHelper openHelper = new SQLiteOpenHelper(
				getApplicationContext(), "test_db", null, 1) {
			public void onCreate(SQLiteDatabase db) {}
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
		};
		return new DbTestHelper(openHelper);
	}
}
