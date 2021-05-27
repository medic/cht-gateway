package medic.gateway.alert;

import android.os.Build;
import android.os.Environment;
import android.test.*;

import java.io.*;

import medic.gateway.alert.test.*;

import okhttp3.mockwebserver.*;

import org.junit.*;

import static org.junit.Assert.*;
import static medic.gateway.alert.test.InstrumentationTestUtils.*;
import static medic.gateway.alert.test.TestUtils.*;

@SuppressWarnings("PMD.SignatureDeclareThrowsException")
public class ExternalLogTest extends AndroidTestCase {
	/**
	 * TODO on Android 6.0+, the permission WRITE_EXTERNAL_STORAGE needs to be
	 * granted at runtime for these tests to function properly.
	 */
	private static final boolean CANNOT_RUN_TESTS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;

	private ExternalLog xLog;

	private DbTestHelper db;

	@BeforeClass
	public void santiseEnvironment() throws Exception {
		removeLogFile();
	}

	@Before
	public void setUp() throws Exception {
		super.setUp();

		xLog = ExternalLog.getInstance(getContext());
		db = new DbTestHelper(getContext());
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();

		removeLogFile();

		db.tearDown();
	}

	@Test
	public void test_shouldProcess_shouldReturnFalse_ifFileMissing() throws Exception {
		if(CANNOT_RUN_TESTS) return;

		// given
		noExistingLog();

		// expect
		assertFalse(xLog.shouldProcess());
	}

	@Test
	public void test_shouldProcess_shouldReturnFalse_ifFileEmpty() throws Exception {
		if(CANNOT_RUN_TESTS) return;

		// given
		existingLog();

		// expect
		assertFalse(xLog.shouldProcess());
	}

	@Test
	public void test_shouldProcess_shouldReturnTrue_ifFileHasContent() throws Exception {
		if(CANNOT_RUN_TESTS) return;

		// given
		existingLog("some content");

		// expect
		assertTrue(xLog.shouldProcess());
	}

	@Test
	public void test_lFalseog_shouldCreateFileIfMissing() throws Exception {
		if(CANNOT_RUN_TESTS) return;

		// given
		removeLogFile();
		noExistingLog();
		assertFalse(logFile().exists());

		// when
		xLog.log(aWtMessage());

		// then
		assertTrue(logFile().exists());
	}

	@Test
	public void test_log_shouldAppendToFile() throws Exception {
		if(CANNOT_RUN_TESTS) return;

		// given
		existingLog("{\"irrelevant\":true}");

		// when
		xLog.log(aWtMessage("abc-123", 1, 2));

		// then
		assertLogContains(
				"{\"irrelevant\":true}",
				"{\"doc\":{\"sms_received\":2,\"sms_sent\":1,\"content\":\"Hello.\",\"from\":\"+447890123123\",\"id\":\"abc-123\"},\"type\":\"wt_message\"}");
	}

	@Test
	public void process_shouldDoNothing_ifLogMissing() throws Exception {
		if(CANNOT_RUN_TESTS) return;

		// given
		noExistingLog();

		// when
		xLog.process(getContext());

		// then
		db.assertEmpty("wt_message");
	}

	@Test
	public void process_shouldDoNothing_ifLogEmpty() throws Exception {
		if(CANNOT_RUN_TESTS) return;

		// given
		existingLog();

		// when
		xLog.process(getContext());

		// then
		db.assertEmpty("wt_message");
	}

	@Test
	public void process_shouldLoadMessagesFromLog() throws Exception {
		if(CANNOT_RUN_TESTS) return;

		// given
		existingLog(
				"{ \"id\":\"abc-123\", \"from\":\"+111\", \"content\":\"message 1\", \"sms_sent\":11, \"sms_received\":111 }",
				"{ \"id\":\"def-456\", \"from\":\"+222\", \"content\":\"message 2\", \"sms_sent\":22, \"sms_received\":222 }",
				"{ \"id\":\"ghi-789\", \"from\":\"+333\", \"content\":\"message 3\", \"sms_sent\":33, \"sms_received\":333 }");

		// when
		xLog.process(getContext());

		// then
		db.assertTable("wt_message",
				"abc-123", "WAITING", 0, "+111", "message 1", 11, 111,
				"def-456", "WAITING", 0, "+222", "message 2", 22, 222,
				"ghi-789", "WAITING", 0, "+333", "message 3", 33, 333);
	}

	@Test
	public void process_shouldIgnoreNonsense() throws Exception {
		if(CANNOT_RUN_TESTS) return;

		// given
		existingLog(
				"any old rubbish", "", "<!-- -->",

				"{ \"id\":\"abc-123\", \"from\":\"+111\", \"content\":\"message 1\", \"sms_sent\":11, \"sms_received\":111 }",

				"{ \"bad-json\":true, \"from\":\"+111\", \"content\":\"message 1\", \"sms_sent\":11, \"sms_received\":111 }",

				"{ \"id\":\"def-456\", \"from\":\"+222\", \"content\":\"message 2\", \"sms_sent\":22, \"sms_received\":222 }",

				"etc. etc.");

		// when
		xLog.process(getContext());

		// then
		db.assertTable("wt_message",
				"abc-123", "WAITING", 0, "+111", "message 1", 11, 111,
				"def-456", "WAITING", 0, "+222", "message 2", 22, 222);
	}

//> PRIVATE HELPERS
	private File logFile() {
		return new File(new File(Environment.getExternalStorageDirectory(), "Documents"), ".cht-gateway.json.log");
	}

	private void removeLogFile() {
		File logFile = logFile();
		if(!logFile.exists()) return;
		boolean success = logFile.delete();
		if(!success) throw new RuntimeException("Failed to delete log file at: " + logFile.getAbsolutePath());
	}

	private void noExistingLog() { /* nothing to do here */ }
	private void existingLog(String... lines) throws Exception {
		File logFile = logFile();

		logFile.getParentFile().mkdirs();
		logFile.createNewFile();

		FileWriter fw = null;
		PrintWriter pw = null;
		try {
			fw = new FileWriter(logFile);
			pw = new PrintWriter(fw);

			for(String line : lines) {
				pw.println(line);
			}
		} finally {
			closeOrThrow(pw);
			closeOrThrow(fw);
		}
	}

	private void assertLogContains(String... expected) throws Exception {
		FileReader fr = null;
		BufferedReader br = null;

		try {
			fr = new FileReader(logFile());
			br = new BufferedReader(fr);

			String actualLine;
			int i = 0;
			while((actualLine = br.readLine()) != null) {
				assertJson("log differs from expected at line " + i,
						expected[i++],
						actualLine);
			}

			if(i < expected.length) {
				fail(String.format("File was too short.  Expected %s lines, but read %s.",
						expected.length, i));
			}
		} finally {
			closeOrThrow(br);
			closeOrThrow(fr);
		}
	}

	private WtMessage aWtMessage() {
		return new WtMessage(A_PHONE_NUMBER, SOME_CONTENT, daysAgo(2));
	}

	private WtMessage aWtMessage(String id, long sent, long received) {
		return new WtMessage(id, WtMessage.Status.WAITING, 0, A_PHONE_NUMBER, SOME_CONTENT, sent, received);
	}

	private void closeOrThrow(Closeable c) throws Exception {
		if(c != null) c.close();
	}
}
