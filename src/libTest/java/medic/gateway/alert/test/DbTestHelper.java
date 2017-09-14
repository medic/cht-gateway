package medic.gateway.alert.test;

import android.content.*;
import android.database.*;
import android.database.sqlite.*;

import java.util.*;
import java.util.regex.*;
import java.lang.reflect.*;

import medic.gateway.alert.*;

import static java.util.UUID.randomUUID;
import static medic.gateway.alert.test.TestUtils.*;
import static org.junit.Assert.*;

@SuppressWarnings({"PMD.JUnit4TestShouldUseAfterAnnotation",
		"PMD.ModifiedCyclomaticComplexity",
		"PMD.SignatureDeclareThrowsException",
		"PMD.StdCyclomaticComplexity",
		"PMD.UseVarargs"})
public class DbTestHelper {
	public static final String[] NO_ARGS = {};
	public static final String ALL_ROWS = null;

	private static final Random RANDOM = new Random();

	private Db db;
	public final SQLiteDatabase raw;

//> CONSTRUCTORS
	public DbTestHelper(SQLiteDatabase raw) {
		this.raw = raw;
	}

	public DbTestHelper(SQLiteOpenHelper sqliteOpenHelper) {
		this.raw = sqliteOpenHelper.getWritableDatabase();
	}

	public DbTestHelper(Context ctx) throws Exception {
		Constructor<?> constructor = Db.class.getDeclaredConstructor(Context.class);
		constructor.setAccessible(true);
		db = (Db) constructor.newInstance(ctx);
		raw = db.getWritableDatabase();
	}

//> ACCESSORS
	public Db getDb() {
		if(db == null) throw new IllegalStateException("Should not be trying to get db unless the DbTestHelper was constructed using a Context");
		return db;
	}

//> TEST METHODS
	public void tearDown() {
		raw.delete("log", ALL_ROWS, NO_ARGS);
		raw.delete("wt_message", ALL_ROWS, NO_ARGS);
		raw.delete("wtm_status", ALL_ROWS, NO_ARGS);
		raw.delete("wt_message_part", ALL_ROWS, NO_ARGS);
		raw.delete("wo_message", ALL_ROWS, NO_ARGS);
		raw.delete("wom_status", ALL_ROWS, NO_ARGS);
		raw.close();
		try {
			Field dbInstanceField = Db.class.getDeclaredField("_instance");
			dbInstanceField.setAccessible(true);
			dbInstanceField.set(null, null);
		} catch(Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public long count(String tableName) {
		return raw.compileStatement("SELECT COUNT(*) FROM " + tableName).simpleQueryForLong();
	}

	public Cursor selectById(String tableName, String[] cols, String id) {
		Cursor c = raw.query(tableName, cols, "_id=?", args(id), null, null, null);
		assertEquals(1, c.getCount());
		c.moveToFirst();
		return c;
	}

	public void insert(String tableName, String[] cols, Object[]... valss) {
		long initialCount = count(tableName);
		for(Object[] vals : valss) {
			ContentValues v = new ContentValues();
			for(int i=cols.length-1; i>=0; --i) {
				if(vals[i] == null) v.put(cols[i], (String) null);
				else if(vals[i] instanceof String) v.put(cols[i], (String) vals[i]);
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
			long rowId = raw.insertOrThrow(tableName, null, v);
			assertEquals(++initialCount, count(tableName));
			assertNotEquals(-1, rowId);
		}
	}

	public void assertTable(String tableName, Object... expectedValues) {
		Cursor c = raw.rawQuery("SELECT * FROM " + tableName, NO_ARGS);
		assertValues(c, expectedValues);
	}

	public void assertValues(String tableName, String[] cols, Object... expectedValues) {
		StringBuilder colBuilder = new StringBuilder();
		for(String col : cols) colBuilder.append(',').append(col);
		Cursor c = raw.rawQuery("SELECT " + colBuilder.substring(1) + " FROM " + tableName, NO_ARGS);

		assertValues(c, expectedValues);
	}

	public void assertCount(String tableName, int expectedCount) {
		assertEquals(expectedCount, count(tableName));
	}

	public void assertEmpty(String tableName) {
		assertCount(tableName, 0);
	}

//> STATIC HELPERS
	@SuppressWarnings("PMD.UnusedPrivateMethod") // looks like a bug in PMD
	private void assertValues(Cursor c, Object... expectedValues) {
		try {
			int colCount = c.getColumnCount();

			if(expectedValues.length % colCount != 0)
				throw new IllegalArgumentException("Wrong number of columns in expected values.");
			int expectedRowCount = expectedValues.length / colCount;

			assertEquals("Wrong number of rows in db.", expectedRowCount, c.getCount());

			for(int i=0; i<expectedRowCount; ++i) {
				c.moveToNext();

				String expectedRow = Arrays.toString(Arrays.copyOfRange(expectedValues, i * colCount, i * colCount + colCount));
				for(int j=0; j<colCount; ++j) {
					Object expected = expectedValues[i * colCount + j];
					String actual = c.getString(j);

					String failMessage = String.format("Unexpected value at row %s column %s.  Expected row: %s.",
							i, j, expectedRow);
					if(expected == null) {
						assertNull(failMessage, actual);
					} else if(expected instanceof Pattern) {
						assertMatches(failMessage, expected, actual);
					} else if(expected instanceof Boolean) {
						String expectedString = ((Boolean) expected) ? "1" : "0";
						assertEquals(failMessage, expectedString, actual);
					} else {
						assertEquals(failMessage, expected.toString(), actual);
					}
				}
			}
		} finally {
			c.close();
		}
	}

	public static String[] args(String... args) {
		return args;
	}

	public static String[] cols(String... columnNames) {
		return columnNames;
	}

	public static Object[] vals(Object... vals) {
		return vals;
	}

	public static String randomUuid() {
		return randomUUID().toString();
	}

	public static long randomLong() {
		return RANDOM.nextLong();
	}
}
