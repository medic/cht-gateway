package medic.gateway;

import android.content.*;
import android.database.sqlite.*;
import android.database.*;
import android.telephony.*;

import java.util.*;

import static java.util.UUID.randomUUID;
import static medic.gateway.BuildConfig.DEBUG;
import static medic.gateway.Utils.*;

public class Db extends SQLiteOpenHelper {
	private static final int VERSION = 1;

	private static final String ALL = null, NO_GROUP = null;
	private static final String[] NO_ARGS = {};

	private static final String tblLOG = "log";
	private static final String LOG_clmTIMESTAMP = "timestamp";
	private static final String LOG_clmMESSAGE = "message";

	private static final String tblWT_MESSAGE = "mt_message";
	private static final String WT_clmID = "id";
	private static final String WT_clmSTATUS = "status";
	private static final String WT_clmLAST_ACTION = "last_action";
	private static final String WT_clmFROM = "_from";
	private static final String WT_clmCONTENT = "content";

	private static final String tblWO_MESSAGE = "mo_message";
	private static final String WO_clmID = "id";
	private static final String WO_clmSTATUS = "status";
	private static final String WO_clmLAST_ACTION = "last_action";
	private static final String WO_clmTO = "_to";
	private static final String WO_clmCONTENT = "content";

	private static final String TRUE = "1";
	private static final String FALSE = "0";

	private static Db INSTANCE;
	public static synchronized Db getInstance(Context ctx) {
		if(INSTANCE == null) {
			INSTANCE = new Db(ctx);
			INSTANCE.init();
			if(DEBUG) INSTANCE.seed();
		}
		return INSTANCE;
	}

	private SQLiteDatabase db;

	private Db(Context ctx) {
		super(ctx, "medic_gateway", null, VERSION);
	}

	public void onCreate(SQLiteDatabase db) {
		this.db = db;

		db.execSQL(String.format("CREATE TABLE %s (" +
					"%s INTEGER NOT NULL PRIMARY KEY, " +
					"%s TEXT NOT NULL)",
				tblLOG, LOG_clmTIMESTAMP, LOG_clmMESSAGE));

		db.execSQL(String.format("CREATE TABLE %s (" +
					"%s TEXT NOT NULL PRIMARY KEY, " +
					"%s TEXT NOT NULL, " +
					"%s INTEGER NOT NULL, " +
					"%s TEXT NOT NULL, " +
					"%s TEXT NOT NULL)",
				tblWT_MESSAGE, WT_clmID, WT_clmSTATUS, WT_clmLAST_ACTION, WT_clmFROM, WT_clmCONTENT));

		db.execSQL(String.format("CREATE TABLE %s (" +
					"%s TEXT NOT NULL PRIMARY KEY, " +
					"%s TEXT NOT NULL, " +
					"%s INTEGER NOT NULL, " +
					"%s TEXT NOT NULL, " +
					"%s TEXT NOT NULL)",
				tblWO_MESSAGE, WO_clmID, WO_clmSTATUS, WO_clmLAST_ACTION, WO_clmTO, WO_clmCONTENT));
	}

	public void init() {
		if(db == null) db = getWritableDatabase();
	}

	public void onUpgrade(SQLiteDatabase db,
			int oldVersion,
			int newVersion) {
	}

//> DebugLogEntry HANDLERS
	void store(DebugLogEntry e) {
		if(DEBUG) log("store() :: storing DebugLogEntry :: %s", e.message);

		ContentValues v = new ContentValues();
		v.put(LOG_clmTIMESTAMP, e.timestamp);
		v.put(LOG_clmMESSAGE, e.message);

		db.insert(tblLOG, null, v);
	}

	List<DebugLogEntry> getLogEntries(int maxCount) {
		Cursor c = null;
		try {
			c = db.query(tblLOG,
					cols(LOG_clmTIMESTAMP, LOG_clmMESSAGE),
					ALL, NO_ARGS,
					NO_GROUP, NO_GROUP,
					SortDirection.DESC.apply(LOG_clmTIMESTAMP),
					Integer.toString(maxCount));

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

//> WoMessage HANDLERS
	boolean store(WoMessage m) {
		if(DEBUG) log("store() :: storing WoMessage :: %s", m);
		long id = db.insert(tblWO_MESSAGE, null, getContentValues(m));
		return id != -1;
	}

	void update(WoMessage m) {
		if(DEBUG) log("update() :: updating WoMessage :: %s", m);
		ContentValues v = getContentValues(m);
		db.update(tblWO_MESSAGE, v, "id=?", args(m.id));
	}

	private ContentValues getContentValues(WoMessage m) {
		ContentValues v = new ContentValues();
		v.put(WO_clmID, m.id);
		v.put(WO_clmSTATUS, m.getStatus().toString());
		v.put(WO_clmLAST_ACTION, m.getLastAction());
		v.put(WO_clmTO, m.to);
		v.put(WO_clmCONTENT, m.content);
		return v;
	}

	WoMessage getWoMessage(String id) {
		List<WoMessage> matches = getWoMessages("id=?", args(id), null, 1);
		if(matches.size() == 0) return null;
		return matches.get(0);
	}

	List<WoMessage> getWoMessages(int maxCount) {
		return getWoMessages(null, null, SortDirection.DESC, maxCount);
	}

	List<WoMessage> getWoMessages(int maxCount, WoMessage.Status status) {
		return getWoMessages("status=?", args(status.toString()), SortDirection.ASC, maxCount);
	}

	private List<WoMessage> getWoMessages(String selection, String[] selectionArgs, SortDirection sort, int maxCount) {
		Cursor c = null;
		try {
			c = db.query(tblWO_MESSAGE,
					cols(WO_clmID, WO_clmSTATUS, WO_clmLAST_ACTION, WO_clmTO, WO_clmCONTENT),
					selection, selectionArgs,
					NO_GROUP, NO_GROUP,
					sort == null? null: sort.apply(WO_clmLAST_ACTION),
					Integer.toString(maxCount));

			int count = c.getCount();
			if(DEBUG) log("getWoMessages() :: item fetch count: %s", count);
			ArrayList<WoMessage> list = new ArrayList(count);
			c.moveToFirst();
			while(count-- > 0) {
				list.add(new WoMessage(
						c.getString(0),
						WoMessage.Status.valueOf(c.getString(1)),
						c.getLong(2),
						c.getString(3),
						c.getString(4)));
				c.moveToNext();
			}
			return list;
		} finally {
			if(c != null) c.close();
		}
	}

//> WtMessage HANDLERS
	boolean store(SmsMessage sms) {
		WtMessage m = new WtMessage(
				sms.getOriginatingAddress(),
				sms.getMessageBody());
		return store(m);
	}

	boolean store(WtMessage m) {
		if(DEBUG) log("store() :: storing WtMessage :: %s", m);
		long id = db.insert(tblWT_MESSAGE, null, getContentValues(m));
		return id != -1;
	}

	void update(Collection<WtMessage> messages) {
		for(WtMessage m : messages) update(m);
	}

	void update(WtMessage m) {
		if(DEBUG) log("update() :: updating WtMessage :: %s", m);
		ContentValues v = getContentValues(m);
		db.update(tblWT_MESSAGE, v, "id=?", args(m.id));
	}

	private ContentValues getContentValues(WtMessage m) {
		ContentValues v = new ContentValues();
		v.put(WT_clmID, m.id);
		v.put(WT_clmSTATUS, m.getStatus().toString());
		v.put(WT_clmLAST_ACTION, m.getLastAction());
		v.put(WT_clmFROM, m.from);
		v.put(WT_clmCONTENT, m.content);
		return v;
	}

	List<WtMessage> getWtMessages(int maxCount) {
		return getWtMessages(null, null, SortDirection.DESC, maxCount);
	}

	List<WtMessage> getWtMessages(int maxCount, WtMessage.Status status) {
		return getWtMessages("status=?", args(status.toString()), SortDirection.ASC, maxCount);
	}

	private List<WtMessage> getWtMessages(String selection, String[] selectionArgs, SortDirection sort, int maxCount) {
		Cursor c = null;
		try {
			c = db.query(tblWT_MESSAGE,
					cols(WT_clmID, WT_clmSTATUS, WT_clmLAST_ACTION, WT_clmFROM, WT_clmCONTENT),
					selection, selectionArgs,
					NO_GROUP, NO_GROUP,
					sort.apply(WT_clmLAST_ACTION),
					Integer.toString(maxCount));

			int count = c.getCount();
			if(DEBUG) log("getWtMessages() :: item fetch count: %s", count);
			ArrayList<WtMessage> list = new ArrayList(count);
			c.moveToFirst();
			while(count-- > 0) {
				list.add(new WtMessage(
						c.getString(0),
						WtMessage.Status.valueOf(c.getString(1)),
						c.getLong(2),
						c.getString(3),
						c.getString(4)));
				c.moveToNext();
			}
			return list;
		} finally {
			if(c != null) c.close();
		}
	}

	private void seed() {
		WtMessages: {
			store(new WtMessage("+254789123123", "hello from kenya"));
			store(new WtMessage("+34678123123", "hello from spain"));
			store(new WtMessage("+447890123123", "hello from uk"));

			for(int i=0; i<20; ++i) {
				store(new WtMessage(randomPhoneNumber(), randomSmsContent()));
			}
		}

		WoMessages: {
			store(new WoMessage(randomUUID().toString(), "+254789123123", "hello kenya"));
			store(new WoMessage(randomUUID().toString(), "+34678123123", "hello spain"));
			store(new WoMessage(randomUUID().toString(), "+447890123123", "hello uk"));

			for(int i=0; i<20; ++i) {
				store(new WoMessage(randomUUID().toString(), randomPhoneNumber(), randomSmsContent()));
			}
		}
	}

//> STATIC HELPERS
	private static String[] cols(String... args) {
		return args;
	}

	private void log(String message, Object... args) {
		if(DEBUG) {
			System.err.println("LOG | Db." + String.format(message, args));
		}
	}
}

enum SortDirection {
	ASC, DESC;

	public String apply(String column) {
		return column + " " + this.toString();
	}
}
