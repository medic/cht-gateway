package medic.gateway;

import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.test.*;

import java.lang.reflect.*;

import org.junit.*;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.*;

@SuppressWarnings("PMD")
public class DbTest extends AndroidTestCase {
	private static final String A_PHONE_NUMBER = "+447890123123";
	private static final String SOME_CONTENT = "Hello.";

	private Db db;

	private Context context;
	private SQLiteDatabase rawDb;

	@Before
	public void setUp() throws Exception {
		super.setUp();

		context = new RenamingDelegatingContext(getContext(), "test_");

		Constructor<?> constructor = Db.class.getDeclaredConstructors()[0];
		constructor.setAccessible(true);
		db = (Db) constructor.newInstance(context);

		rawDb = db.getWritableDatabase();
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
		db.close();
	}

	@Test
	public void test_classParamsShouldBeInitialised() {
		assertNotNull(db);
		assertNotNull(context);
		assertNotNull(rawDb);
	}

	@Test
	public void test_canStoreWtMessages() {
		// given
		assertEquals(0, count("wt_message"));
		WtMessage m = aMessageWith(WtMessage.Status.WAITING);

		// when
		boolean successReported = db.store(m);

		// then
		assertTrue(successReported);
		assertEquals(1, count("wt_message"));
	}

	@Test
	public void test_updateStatusFrom_shouldFailSilentlyIfMessageNotInDb() {
		// given
		WtMessage unsavedMessage = aMessageWith(WtMessage.Status.FORWARDED);

		// when
		db.updateStatusFrom(WtMessage.Status.WAITING, unsavedMessage);

		// then
		assertEquals(0, count("wt_message"));
	}

	@Test
	public void test_updateStatusFrom_shouldFailSilentlyIfWrongStatusFound() {
		// given
		String id = randomUUID().toString();
		insert("wt_message",
				cols("id", "status", "last_action", "_from", "content"),
				vals(id, WtMessage.Status.FORWARDED, 0, A_PHONE_NUMBER, SOME_CONTENT));
		WtMessage messageWithUpdatedStatus = aMessageWith(id, WtMessage.Status.FAILED);

		// when
		db.updateStatusFrom(WtMessage.Status.WAITING, messageWithUpdatedStatus);

		// then
		Cursor c = selectById("wt_message", cols("status", "last_action"), id);
		assertEquals("FORWARDED", c.getString(0));
		assertEquals(0, c.getLong(1));
	}

	@Test
	public void test_updateStatusFrom_shouldUpdateStatusOfMatchedMessageIfExpectedStatusFound() {
		// given
		String id = randomUUID().toString();
		insert("wt_message",
				cols("id", "status", "last_action", "_from", "content"),
				vals(id, WtMessage.Status.WAITING, 0, A_PHONE_NUMBER, SOME_CONTENT));
		WtMessage messageWithUpdatedStatus = aMessageWith(id, WtMessage.Status.FORWARDED);

		// when
		db.updateStatusFrom(WtMessage.Status.WAITING, messageWithUpdatedStatus);

		// then
		Cursor c = selectById("wt_message", cols("status", "last_action"), id);
		assertEquals("FORWARDED", c.getString(0));
		assertNotEquals(0, c.getLong(1));
	}

	@Test
	public void test_canStoreWoMessages() {
		// given
		assertEquals(0, count("wo_message"));
		WoMessage m = aMessageWith(WoMessage.Status.PENDING);

		// when
		boolean successReported = db.store(m);

		// then
		assertTrue(successReported);
		assertEquals(1, count("wo_message"));
	}

	@Test
	public void test_updateStatus_shouldFailSilentlyIfMessageNotInDb() {
		// given
		WoMessage unsavedMessage = aMessageWith(WoMessage.Status.PENDING);

		// when
		db.updateStatus(unsavedMessage, WoMessage.Status.PENDING, WoMessage.Status.DELIVERED);

		// then
		assertEquals(0, count("wt_message"));
	}

	@Test
	public void test_updateStatus_shouldFailSilentlyIfWrongStatusFound() {
		// given
		String id = randomUUID().toString();
		insert("wo_message",
				cols("id", "status", "status_needs_forwarding", "last_action", "_to", "content"),
				vals(id, WoMessage.Status.REJECTED, 0, 0, A_PHONE_NUMBER, SOME_CONTENT));
		WoMessage messageWithUpdatedStatus = aMessageWith(id, WoMessage.Status.PENDING);

		// when
		db.updateStatus(messageWithUpdatedStatus, WoMessage.Status.PENDING, WoMessage.Status.DELIVERED);

		// then
		Cursor c = selectById("wo_message", cols("status", "last_action"), id);
		assertEquals("REJECTED", c.getString(0));
		assertEquals(0, c.getLong(1));
	}

	@Test
	public void test_updateStatus_shouldUpdateStatusOfMatchedMessageIfExpectedStatusFound() {
		// given
		String id = randomUUID().toString();
		insert("wo_message",
				cols("id", "status", "status_needs_forwarding", "last_action", "_to", "content"),
				vals(id, WoMessage.Status.PENDING, 0, 0, A_PHONE_NUMBER, SOME_CONTENT));
		WoMessage messageWithUpdatedStatus = aMessageWith(id, WoMessage.Status.PENDING);

		// when
		db.updateStatus(messageWithUpdatedStatus, WoMessage.Status.PENDING, WoMessage.Status.DELIVERED);

		// then
		Cursor c = selectById("wo_message", cols("status", "last_action"), id);
		assertEquals("DELIVERED", c.getString(0));
		assertNotEquals(0, c.getLong(1));
	}

//> HELPERS
	private long count(String tableName) {
		return rawDb.compileStatement("SELECT COUNT(*) FROM " + tableName).simpleQueryForLong();
	}

	private Cursor selectById(String tableName, String[] cols, String id) {
		Cursor c = rawDb.query(tableName, cols, "id=?", args(id), null, null, null);
		assertEquals(1, c.getCount());
		c.moveToFirst();
		return c;
	}

	private void insert(String tableName, String[] cols, Object[] vals) {
		ContentValues v = new ContentValues();
		long initialCount = count(tableName);
		for(int i=cols.length-1; i>=0; --i) {
			if(vals[i] instanceof String) v.put(cols[i], (String) vals[i]);
			else if(vals[i] instanceof Byte) v.put(cols[i], (Byte) vals[i]);
			else if(vals[i] instanceof Short) v.put(cols[i], (Short) vals[i]);
			else if(vals[i] instanceof Integer) v.put(cols[i], (Integer) vals[i]);
			else if(vals[i] instanceof Long) v.put(cols[i], (Long) vals[i]);
			else if(vals[i] instanceof Float) v.put(cols[i], (Float) vals[i]);
			else if(vals[i] instanceof Double) v.put(cols[i], (Double) vals[i]);
			else if(vals[i] instanceof Boolean) v.put(cols[i], (Boolean) vals[i]);
			else if(vals[i] instanceof byte[]) v.put(cols[i], (byte[]) vals[i]);
			else v.put(cols[i], vals[i].toString());
		}
		long rowId = rawDb.insert(tableName, null, v);
		assertEquals(initialCount+1, count(tableName));
		assertNotEquals(-1, rowId);
	}

//> STATIC HELPERS
	private static String[] args(String... args) {
		return args;
	}

	private static String[] cols(String... columnNames) {
		return columnNames;
	}

	private static Object[] vals(Object... vals) {
		return vals;
	}

	private static WtMessage aMessageWith(WtMessage.Status status) {
		return aMessageWith(randomUUID().toString(), status);
	}

	private static WtMessage aMessageWith(String id, WtMessage.Status status) {
		return new WtMessage(id, status, System.currentTimeMillis(), A_PHONE_NUMBER, SOME_CONTENT);
	}

	private static WoMessage aMessageWith(WoMessage.Status status) {
		return aMessageWith(randomUUID().toString(), status);
	}

	private static WoMessage aMessageWith(String id, WoMessage.Status status) {
		return new WoMessage(id, status, System.currentTimeMillis(), A_PHONE_NUMBER, SOME_CONTENT);
	}
}
