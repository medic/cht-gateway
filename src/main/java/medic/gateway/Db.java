package medic.gateway;

import android.content.*;
import android.database.sqlite.*;
import android.database.*;

import java.util.*;

import static medic.gateway.BuildConfig.DEBUG;

public class Db extends SQLiteOpenHelper {
	private static final int VERSION = 1;
	private static final String tblLOG = "log";

	private static final String LOG_clmTIMESTAMP = "timestamp";
	private static final String LOG_clmMESSAGE = "message";

	private static final String TRUE = "1";
	private static final String FALSE = "0";

	private static Db INSTANCE;
	public static synchronized Db getInstance(Context ctx) {
		if(INSTANCE == null) {
			INSTANCE = new Db(ctx);
			INSTANCE.init();
		}
		return INSTANCE;
	}

	private SQLiteDatabase db;

	private Db(Context ctx) {
		super(ctx, "medic_gateway", null, VERSION);
	}

	public void onCreate(SQLiteDatabase db) {
		this.db = db;

		db.execSQL(String.format(
				"CREATE TABLE %s (" +
					"%s INTEGER NOT NULL PRIMARY KEY, " +
					"%s TEXT NOT NULL)",
				tblLOG, LOG_clmTIMESTAMP, LOG_clmMESSAGE));
	}

	public void init() {
		if(db == null) db = getWritableDatabase();
	}

	public void onUpgrade(SQLiteDatabase db,
			int oldVersion,
			int newVersion) {
	}

	void store(DebugLogEntry e) {
		if(DEBUG) log("store() :: storing DebugLogEntry :: %s", e.message);

		ContentValues v = new ContentValues();
		v.put(LOG_clmTIMESTAMP, e.timestamp);
		v.put(LOG_clmMESSAGE, e.message);

		db.insert(tblLOG, null, v);
	}

	List<DebugLogEntry> getLogEntries(int maxCount) {
		String q = String.format("SELECT %s,%s FROM %s ORDER BY %s DESC LIMIT %s",
				LOG_clmTIMESTAMP, LOG_clmMESSAGE, tblLOG, LOG_clmTIMESTAMP, maxCount);
		Cursor c = null;
		try {
			c = db.rawQuery(q, args());

			int count = c.getCount();
			if(DEBUG) log("getLogEntries() :: item fetch count: %s", count);
			ArrayList<DebugLogEntry> list = new ArrayList(count);
			c.moveToFirst();
			while(count-- > 0) {
				list.add(new DebugLogEntry(
						c.getLong(0),
						c.getString(1)));
				c.moveToNext();
			}
			return list;
		} finally {
			if(c != null) c.close();
		}
	}

	private static String[] args(String... args) {
		return args;
	}

	private void log(String message, Object... args) {
		if(DEBUG) {
			System.err.println("LOG | Db." + String.format(message, args));
		}
	}
}
