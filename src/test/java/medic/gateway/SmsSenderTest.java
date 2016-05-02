package medic.gateway;

import medic.gateway.WoMessage.Status;
import medic.gateway.test.*;

import org.junit.*;
import org.junit.runner.*;
import org.robolectric.*;
import org.robolectric.annotation.*;

import static medic.gateway.WoMessage.Status.*;
import static medic.gateway.test.DbTestHelper.*;
import static medic.gateway.test.TestUtils.*;
import static org.junit.Assert.*;

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

	@Test
	public void sendUnsentSmses_shouldOnlyTryToSendUnentSms() {
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
		db.assertTable("wo_message",
				"id-UNSENT",    "PENDING",   true,  ANY_NUMBER, "+1", "testing: UNSENT",
				"id-PENDING",   "PENDING",   false, 0,          "+1", "testing: PENDING",
				"id-SENT",      "SENT",      false, 0,          "+1", "testing: SENT",
				"id-FAILED",    "FAILED",    false, 0,          "+1", "testing: FAILED",
				"id-REJECTED",  "REJECTED",  false, 0,          "+1", "testing: REJECTED",
				"id-DELIVERED", "DELIVERED", false, 0,          "+1", "testing: DELIVERED");
	}
}
