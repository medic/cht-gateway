package medic.gateway.alert;

import android.telephony.*;
import java.lang.reflect.*;
import java.util.*;
import medic.gateway.alert.WoMessage.Status;
import medic.gateway.alert.test.*;
import org.junit.*;
import org.junit.runner.*;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.*;
import static androidx.test.core.app.ApplicationProvider.*;
import static medic.gateway.alert.WoMessage.Status.*;
import static medic.gateway.alert.test.DbTestHelper.*;
import static medic.gateway.alert.test.TestUtils.*;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=26)
@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert",
		"PMD.UnnecessaryFullyQualifiedName",
		"PMD.SignatureDeclareThrowsException"})
public class SmsSenderTest {
	private DbTestHelper db;

	private SmsSender smsSender;

	@Before
	public void setUp() throws Exception {
		db = new DbTestHelper(getApplicationContext());

		smsSender = new SmsSender(getApplicationContext());
	}

	@After
	public void tearDown() throws Exception {
		db.tearDown();
	}

	@Test
	public void sendUnsentSmses_shouldOnlyTryToSendUnsentSms() {
		// given
		for(Status s : Status.values()) {
			// create a WoMessage with each status
			db.insert("wo_message",
					cols("_id",     "status", "failure_reason", "last_action", "_to", "content", "retries"),
					vals("id-" + s, s,        null,             0,             "+1",  "testing: " + s, 0));
		}

		// when
		smsSender.sendUnsentSmses();

		// then
		db.assertTable("wo_message",
				"id-UNSENT",    "PENDING",   null, ANY_NUMBER, "+1", "testing: UNSENT", 0,
				"id-PENDING",   "PENDING",   null, 0,          "+1", "testing: PENDING", 0,
				"id-SENT",      "SENT",      null, 0,          "+1", "testing: SENT", 0,
				"id-FAILED",    "FAILED",    null, 0,          "+1", "testing: FAILED", 0,
				"id-DELIVERED", "DELIVERED", null, 0,          "+1", "testing: DELIVERED", 0);
	}

	@Test
	public void sendUnsentSmses_shouldRetryToSendUnsentSmsWhenItsTime() {
		// given
		long lastAction = System.currentTimeMillis();
		long lastActionPast = lastAction - 180000; // 3 min in milliseconds

		db.insert("wo_message",
				cols("_id", "status", "failure_reason", "last_action", "_to", "content", "retries"),
				// It's not time to retry yet
				vals("id-UNSENT-1", UNSENT, null, lastAction, "+1", "testing", 5),
				// It's time, ready to send
				vals("id-UNSENT-2", UNSENT, null, lastActionPast, "+1", "testing", 2),
				// New sms, not retry flow.
				vals("id-UNSENT-3", UNSENT, null, lastAction, "+1", "testing", 0),
				// No retry, exceeding maximum number of retries.
				vals("id-UNSENT-4", UNSENT, null, lastAction, "+1", "testing", 21));

		// when
		smsSender.sendUnsentSmses();

		// then
		db.assertTable("wo_message",
				"id-UNSENT-1", UNSENT, null, ANY_NUMBER, "+1", "testing", 5,
				"id-UNSENT-2", PENDING, null, ANY_NUMBER, "+1", "testing", 2,
				"id-UNSENT-3", PENDING, null, ANY_NUMBER, "+1", "testing", 0,
				"id-UNSENT-4", UNSENT, null, ANY_NUMBER, "+1", "testing", 21);
	}

	@Test
	public void sendUnsentSmses_shouldPassUnsentMessagesToSmsManager() {
		// given
		for(Status s : Status.values()) {
			// create a WoMessage with each status
			db.insert("wo_message",
					cols("_id",     "status", "last_action", "_to", "content", "retries"),
					vals("id-" + s, s,        0,             "+1",  "testing: " + s, 0));
		}

		// when
		smsSender.sendUnsentSmses();

		// then
		assertSmsSent("+1", "testing: UNSENT");
	}

	@Test
	public void divideMessageForCdma_ascii_shouldLeaveSinglePartMessagesAlone() {
		// given
		String messageContent =
				"single message of 140 valid ascii chars ------------------------------" +
				"----------------------------------------------------------------------";

		// when
		ArrayList<String> actual = SmsSender.divideMessageForCdma(messageContent);

		// then
		assertListEquals(actual, messageContent);
	}

	@Test
	public void divideMessageForCdma_ascii_shouldDivideMultipartMessages() {
		// given
		String messageContent =
				"part 1 starts here: message of 408 valid ascii chars -----------------------------------------------------------------------------------" +
				"part 2 starts here: --------------------------------------------------------------------------------------------------------------------" +
				"part 3 starts here: --------------------------------------------------------------------------------------------------------------------";

		// when
		ArrayList<String> actual = SmsSender.divideMessageForCdma(messageContent);

		// then
		assertListEquals(actual,
				"1/3 part 1 starts here: message of 408 valid ascii chars -----------------------------------------------------------------------------------",
				"2/3 part 2 starts here: --------------------------------------------------------------------------------------------------------------------",
				"3/3 part 3 starts here: --------------------------------------------------------------------------------------------------------------------");
	}

	@Test
	public void divideMessageForCdma_utf16_shouldLeaveSinglePartMessagesAlone() {
		// given
		String messageContent = "single message of 70 valid utf16 chars (नेपाल) 01234567890123456789012";

		// when
		ArrayList<String> actual = SmsSender.divideMessageForCdma(messageContent);

		// then
		assertListEquals(actual, messageContent);
	}

	@Test
	public void divideMessageForCdma_utf16_shouldDivideMultipartMessages() {
		// given
		// N.B. string widths may appear weird - some of the "chars" are actually diacritics
		String messageContent =
				"part 1 starts here: message of 198 valid utf16 chars (नेपाल) -----" +
				"part 2 starts here: ----------------------------------------------" +
				"part 3 starts here: ----------------------------------------------";

		// when
		ArrayList<String> actual = SmsSender.divideMessageForCdma(messageContent);

		// then
		assertListEquals(actual,
				"1/3 part 1 starts here: message of 198 valid utf16 chars (नेपाल) -----",
				"2/3 part 2 starts here: ----------------------------------------------",
				"3/3 part 3 starts here: ----------------------------------------------");
	}

	@Test public void dummySendMode_noMessagesShouldBeSent() throws Exception {
		// given
		enableDummySendMode();
		db.insert("wo_message",
				cols("_id",    "status", "failure_reason", "last_action", "_to", "content", "retries"),
				vals("id-123", UNSENT,   null,             0,             "+1",  "testing", 0));

		// when
		smsSender.sendUnsentSmses();

		// then
		db.assertTable("wo_message",
				"id-123", "DELIVERED", null, ANY_NUMBER, "+1", "testing", 0);
		db.assertTable("wom_status",
				ANY_ID, "id-123", "PENDING",   null, ANY_NUMBER, true,
				ANY_ID, "id-123", "SENT",      null, ANY_NUMBER, true,
				ANY_ID, "id-123", "DELIVERED", null, ANY_NUMBER, true);
	}

//> PRIVATE HELPERS
	private void assertSmsSent(String to, String expectedContent) {
		ShadowSmsManager shadowSmsManager = shadowOf(SmsManager.getDefault());
		ShadowSmsManager.TextMultipartParams last = shadowSmsManager.getLastSentMultipartTextMessageParams();

		assertNotNull(last);

		assertEquals(to, last.getDestinationAddress());

		StringBuilder actualContent = new StringBuilder();
		for(String part : last.getParts()) actualContent.append(part);
		assertEquals(expectedContent, actualContent.toString());
	}

	private void enableDummySendMode() throws Exception {
		// This reflection is ugly, but helps keep the prod code clean
		Field dummySendMode = SmsSender.class.getDeclaredField("dummySendMode");
		dummySendMode.setAccessible(true);
		dummySendMode.set(smsSender, true);
	}
}
