package medic.gateway;

import android.telephony.*;

import medic.gateway.WoMessage.Status;
import medic.gateway.test.*;

import org.junit.*;
import org.junit.runner.*;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.*;

import static medic.gateway.WoMessage.Status.*;
import static medic.gateway.test.DbTestHelper.*;
import static medic.gateway.test.TestUtils.*;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.*;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants=BuildConfig.class)
@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert",
		"PMD.UnnecessaryFullyQualifiedName"})
public class SmsSenderTest {
	private DbTestHelper db;

	private SmsSender smsSender;

	@Before
	public void setUp() throws Exception {
		db = new DbTestHelper(RuntimeEnvironment.application);

		smsSender = new SmsSender(RuntimeEnvironment.application);
	}

	@After
	public void tearDown() throws Exception {
		db.tearDown();
	}

	@Test
	public void sendUnsentSmses_shouldOnlyTryToSendUnentSms() {
		// given
		for(Status s : Status.values()) {
			// create a WoMessage with each status
			db.insert("wo_message",
					cols("_id",     "status", "failure_reason", "status_needs_forwarding", "last_action", "_to", "content"),
					vals("id-" + s, s,        null,             false,                     0,             "+1",  "testing: " + s));
		}

		// when
		smsSender.sendUnsentSmses();

		// then
		db.assertTable("wo_message",
				"id-UNSENT",    "PENDING",   true,  null,  ANY_NUMBER, "+1", "testing: UNSENT",
				"id-PENDING",   "PENDING",   false, null, 0,          "+1", "testing: PENDING",
				"id-SENT",      "SENT",      false, null, 0,          "+1", "testing: SENT",
				"id-FAILED",    "FAILED",    false, null, 0,          "+1", "testing: FAILED",
				"id-DELIVERED", "DELIVERED", false, null, 0,          "+1", "testing: DELIVERED");
	}

	@Test
	public void sendUnsentSmses_shouldPassUnsentMessagesToSmsManager() {
		// given
		for(Status s : Status.values()) {
			// create a WoMessage with each status
			db.insert("wo_message",
					cols("_id", "status", "status_needs_forwarding", "last_action", "_to", "content"),
					vals("id-" + s, s, false, 0, "+1", "testing: " + s));
		}

		// when
		smsSender.sendUnsentSmses();

		// then
		assertSmsSent("+1", "testing: UNSENT");
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
}
