package medic.gateway.test;

import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import android.test.*;

import java.lang.reflect.*;
import java.util.concurrent.*;
import java.util.regex.*;

import medic.gateway.*;

import okhttp3.mockwebserver.*;

import org.junit.*;

import static org.junit.Assert.*;
import static medic.gateway.test.TestUtils.*;

public abstract class HttpTestCase extends AndroidTestCase {
	protected MockWebServer server;

	protected SQLiteDatabase db;

	@Before
	public void setUp() throws Exception {
		super.setUp();

		this.server = new MockWebServer();
		server.start();

		SharedPreferences.Editor ed = getContext()
				.getSharedPreferences(SettingsStore.class.getName(), Context.MODE_PRIVATE)
				.edit();
		ed.putString("app-url", serverUrl());
		assertTrue(ed.commit());

		Constructor<?> constructor = Db.class.getDeclaredConstructors()[0];
		constructor.setAccessible(true);
		db = ((Db) constructor.newInstance(getContext())).getWritableDatabase();
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
		server.shutdown();

		db.delete("log", ALL_ROWS, NO_ARGS);
		db.delete("wt_message", ALL_ROWS, NO_ARGS);
		db.delete("wo_message", ALL_ROWS, NO_ARGS);
	}

//> DB HELPERS
	protected void dbTableEmpty(String tableName) {
		Cursor c = getDbContents(tableName);
		try {
			assertEquals(0, c.getCount());
		} finally {
			c.close();
		}
	}

	protected void dbTableContains(String tableName, Object... expectedValues) {
		Cursor c = getDbContents(tableName);
		try {
			int colCount = c.getColumnCount();
			int expectedRowCount = expectedValues.length / colCount;
			assertEquals("Wrong number of rows in db.", expectedRowCount, c.getCount());
			for(int i=0; i<expectedRowCount; ++i) {
				c.moveToNext();
				for(int j=0; j<colCount; ++j) {
					Object expected = expectedValues[i * colCount + j];
					String actual = c.getString(j);
					if(expected instanceof Pattern) {
						assertMatches("Unexpected value at row " + i + " column " + j,
								expected, actual);
					} else if(expected instanceof Boolean) {
						String expectedString = ((Boolean) expected) ? "1" : "0";
						assertEquals("Unexpected value at row " + i + " column " + j,
								expectedString, actual);
					} else {
						assertEquals("Unexpected value at row " + i + " column " + j,
								expected.toString(), actual);
					}
				}
			}
		} finally {
			c.close();
		}
	}

	private Cursor getDbContents(String tableName) {
		return db.rawQuery("SELECT * FROM " + tableName, NO_ARGS);
	}

//> HTTP HELPERS
	protected void nextResponseJson(String jsonString) {
		server.enqueue(new MockResponse().setBody(jsonString));
	}

	protected void nextResponseError(int httpResponseCode) {
		server.enqueue(new MockResponse().setResponseCode(httpResponseCode));
	}

	protected String serverUrl() {
		return server.url("/api").toString();
	}

	protected void assertSingleGetRequestMade() {
		RecordedRequest r = nextRequest();
		assertEquals("GET /api HTTP/1.1", r.getRequestLine());
		assertEquals("application/json", r.getHeader("Content-Type"));
		assertNull(nextRequest());
	}

	protected RecordedRequest assertSinglePostRequestMade() {
		RecordedRequest r = nextRequest();
		assertEquals("POST /api HTTP/1.1", r.getRequestLine());
		assertEquals("application/json", r.getHeader("Content-Type"));
		assertNull(nextRequest());
		return r;
	}

	protected RecordedRequest nextRequest() {
		try {
			return server.takeRequest(1, TimeUnit.MILLISECONDS);
		} catch(InterruptedException ex) {
			return null;
		}
	}
}
