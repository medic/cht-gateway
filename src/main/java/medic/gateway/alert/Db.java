package medic.gateway.alert;

import android.content.*;
import android.database.sqlite.*;
import android.database.*;
import android.telephony.*;

import java.util.*;

import static java.util.UUID.randomUUID;
import static medic.gateway.alert.BuildConfig.DEBUG;
import static medic.gateway.alert.BuildConfig.LOAD_SEED_DATA;
import static medic.gateway.alert.GatewayLog.*;
import static medic.gateway.alert.Utils.*;

@SuppressWarnings("PMD.GodClass")
public final class Db extends SQLiteOpenHelper {
	private static final int VERSION = 1;

	private static final String ALL = null, NO_GROUP = null;
	private static final String[] NO_ARGS = {};

	private static final String tblLOG = "log";
	private static final String LOG_clmID = "_id";
	private static final String LOG_clmTIMESTAMP = "timestamp";
	private static final String LOG_clmMESSAGE = "message";

	private static final String tblWT_MESSAGE = "wt_message";
	private static final String WT_clmID = "_id";
	private static final String WT_clmSTATUS = "status";
	private static final String WT_clmLAST_ACTION = "last_action";
	private static final String WT_clmFROM = "_from";
	private static final String WT_clmCONTENT = "content";

	private static final String tblWO_MESSAGE = "wo_message";
	private static final String WO_clmID = "_id";
	private static final String WO_clmSTATUS = "status";
	private static final String WO_clmSTATUS_NEEDS_FORWARDING = "status_needs_forwarding";
	private static final String WO_clmFAILURE_REASON = "failure_reason";
	private static final String WO_clmLAST_ACTION = "last_action";
	private static final String WO_clmTO = "_to";
	private static final String WO_clmCONTENT = "content";

	private static final String TRUE = "1";
	private static final String FALSE = "0";

	private static Db _instance;
	public static synchronized Db getInstance(Context ctx) { // NOPMD
		if(_instance == null) {
			_instance = new Db(ctx);
			if(LOAD_SEED_DATA &&
					_instance.db.compileStatement("SELECT COUNT(*) FROM " + tblLOG).simpleQueryForLong() == 0) {
				_instance.seed();
			}

			if(DEBUG) _instance.storeLogEntry("LOG MESSAGES: " + _instance.db.compileStatement("SELECT COUNT(*) FROM " + tblLOG).simpleQueryForLong());
			if(DEBUG) _instance.storeLogEntry("WO MESSAGES: " + _instance.db.compileStatement("SELECT COUNT(*) FROM " + tblWO_MESSAGE).simpleQueryForLong());
			if(DEBUG) _instance.storeLogEntry("WT MESSAGES: " + _instance.db.compileStatement("SELECT COUNT(*) FROM " + tblWT_MESSAGE).simpleQueryForLong());
		}

		return _instance;
	}

	private final SQLiteDatabase db; // NOPMD

	private Db(Context ctx) {
		super(ctx, "medic_gateway", null, VERSION);
		db = getWritableDatabase();
	}

	public void onCreate(SQLiteDatabase db) {
		db.execSQL(String.format("CREATE TABLE %s (" +
					"%s INTEGER PRIMARY KEY, " +
					"%s INTEGER NOT NULL, " +
					"%s TEXT NOT NULL)",
				tblLOG, LOG_clmID, LOG_clmTIMESTAMP, LOG_clmMESSAGE));

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
					"%s TEXT, " +
					"%s INTEGER NOT NULL, " +
					"%s TEXT NOT NULL, " +
					"%s TEXT NOT NULL)",
				tblWO_MESSAGE, WO_clmID, WO_clmSTATUS, WO_clmSTATUS_NEEDS_FORWARDING, WO_clmFAILURE_REASON, WO_clmLAST_ACTION, WO_clmTO, WO_clmCONTENT));
	}

	public void onUpgrade(SQLiteDatabase db,
			int oldVersion,
			int newVersion) {
		// Handle DB upgrades here, once we start supporting released versions
	}

//> GatewayEventLogEntry HANDLERS
	void storeLogEntry(String message) {
		ContentValues v = new ContentValues();
		v.put(LOG_clmTIMESTAMP, System.currentTimeMillis());
		v.put(LOG_clmMESSAGE, message);

		db.insert(tblLOG, null, v);
	}

	Cursor getLogEntries(int maxCount) {
		return db.query(tblLOG,
				cols(LOG_clmID, LOG_clmTIMESTAMP, LOG_clmMESSAGE),
				ALL, NO_ARGS,
				NO_GROUP, NO_GROUP,
				SortDirection.DESC.apply(LOG_clmTIMESTAMP),
				Integer.toString(maxCount));
	}

//> WoMessage HANDLERS
	boolean store(WoMessage m) {
		log("store() :: %s", m);
		long id = db.insert(tblWO_MESSAGE, null, getContentValues(m));
		return id != -1;
	}

	void setFailed(WoMessage m, String failureReason) {
		updateStatus(m, WoMessage.Status.PENDING, WoMessage.Status.FAILED, failureReason);
	}

	boolean updateStatus(WoMessage m, WoMessage.Status newStatus) {
		return updateStatus(m, m.status, newStatus);
	}

	boolean updateStatus(WoMessage m, WoMessage.Status oldStatus, WoMessage.Status newStatus) {
		return updateStatus(m, oldStatus, newStatus, null);
	}

	private boolean updateStatus(WoMessage m, WoMessage.Status oldStatus, WoMessage.Status newStatus, String failureReason) {
		log("updateStatus() :: %s :: %s -> %s (%s)", m, oldStatus, newStatus, failureReason);

		if((newStatus == WoMessage.Status.FAILED) == (failureReason == null))
			throw new IllegalArgumentException(String.format(
					"Give failureReason iff new status == FAILED (newStatus=%s, failureReason=%s)",
					newStatus,
					failureReason));

		ContentValues v = new ContentValues();
		v.put(WO_clmSTATUS, newStatus.toString());
		v.put(WO_clmSTATUS_NEEDS_FORWARDING, TRUE);
		v.put(WO_clmFAILURE_REASON, failureReason);
		v.put(WO_clmLAST_ACTION, System.currentTimeMillis());

		int affected;
		if(oldStatus == null) {
			affected = db.update(tblWO_MESSAGE, v, eq(WO_clmID), args(m.id));
		} else {
			affected = db.update(tblWO_MESSAGE, v, eq(WO_clmID, WO_clmSTATUS), args(m.id, oldStatus));
		}
		return affected > 0;
	}

	void setStatusForwarded(WoMessage m) {
		log("setStatusForwarded() :: %s", m);

		ContentValues v = new ContentValues();
		v.put(WO_clmSTATUS_NEEDS_FORWARDING, FALSE);

		db.update(tblWO_MESSAGE, v, eq(WO_clmID, WO_clmSTATUS), args(m.id, m.status));
	}

	private ContentValues getContentValues(WoMessage m) {
		ContentValues v = new ContentValues();
		v.put(WO_clmID, m.id);
		v.put(WO_clmSTATUS, m.status.toString());
		v.put(WO_clmSTATUS_NEEDS_FORWARDING, FALSE);
		v.put(WO_clmFAILURE_REASON, m.status == WoMessage.Status.FAILED ? m.getFailureReason() : null);
		v.put(WO_clmLAST_ACTION, System.currentTimeMillis());
		v.put(WO_clmTO, m.to);
		v.put(WO_clmCONTENT, m.content);
		return v;
	}

	WoMessage getWoMessage(String id) {
		List<WoMessage> matches = getWoMessages(eq(WO_clmID), args(id), null, 1);
		if(matches.isEmpty()) return null;
		return matches.get(0);
	}

	Cursor getWoMessages(int maxCount) {
		return getWoMessageCursor(null, null, SortDirection.DESC, maxCount);
	}

	List<WoMessage> getWoMessages(int maxCount, WoMessage.Status status) {
		return getWoMessages(eq(WO_clmSTATUS), args(status), SortDirection.ASC, maxCount);
	}

	List<WoMessage> getWoMessagesWithStatusChanges(int maxCount) {
		return getWoMessages(eq(WO_clmSTATUS_NEEDS_FORWARDING), args(TRUE), SortDirection.ASC, maxCount);
	}

	private List<WoMessage> getWoMessages(String selection, String[] selectionArgs, SortDirection sort, int maxCount) {
		Cursor c = null;
		try {
			c = getWoMessageCursor(selection, selectionArgs, sort, maxCount);

			int count = c.getCount();
			log("getWoMessages() :: item fetch count: %s", count);
			ArrayList<WoMessage> list = new ArrayList<>(count);
			c.moveToFirst();
			while(count-- > 0) {
				list.add(woMessageFrom(c));
				c.moveToNext();
			}
			return list;
		} finally {
			if(c != null) c.close();
		}
	}

	private Cursor getWoMessageCursor(String selection, String[] selectionArgs, SortDirection sort, int maxCount) {
		return db.query(tblWO_MESSAGE,
				cols(WO_clmID, WO_clmSTATUS, WO_clmFAILURE_REASON, WO_clmLAST_ACTION, WO_clmTO, WO_clmCONTENT),
				selection, selectionArgs,
				NO_GROUP, NO_GROUP,
				sort == null? null: sort.apply(WO_clmLAST_ACTION),
				Integer.toString(maxCount));
	}

	public static WoMessage woMessageFrom(Cursor c) {
		String id = c.getString(0);
		WoMessage.Status status = WoMessage.Status.valueOf(c.getString(1));
		String failureReason = c.getString(2);
		long lastAction = c.getLong(3);
		String to = c.getString(4);
		String content = c.getString(5);

		return new WoMessage(id, status, failureReason, lastAction, to, content);
	}

//> WtMessage HANDLERS
	boolean store(MultipartSms sms) {
		WtMessage m = new WtMessage(
				sms.getOriginatingAddress(),
				sms.getMessageBody());
		return store(m);
	}

	boolean store(SmsMessage sms) {
		WtMessage m = new WtMessage(
				sms.getOriginatingAddress(),
				sms.getMessageBody());
		return store(m);
	}

	boolean store(WtMessage m) {
		log("store() :: %s", m);
		long id = db.insert(tblWT_MESSAGE, null, getContentValues(m));
		return id != -1;
	}

	void updateStatusFrom(WtMessage.Status oldStatus, WtMessage m) {
		log("updateStatusFrom() :: %s :: %s -> %s", m, oldStatus, m.getStatus());

		ContentValues v = new ContentValues();
		v.put(WT_clmSTATUS, m.getStatus().toString());
		v.put(WT_clmLAST_ACTION, m.getLastAction());

		db.update(tblWT_MESSAGE, v, eq(WT_clmID, WT_clmSTATUS), args(m.id, oldStatus));
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

	WtMessage getWtMessage(String id) {
		List<WtMessage> matches = getWtMessages(eq(WO_clmID), args(id), null, 1);
		if(matches.isEmpty()) return null;
		return matches.get(0);
	}

	Cursor getWtMessages(int maxCount) {
		return getWtMessageCursor(null, null, SortDirection.DESC, maxCount);
	}

	List<WtMessage> getWtMessages(int maxCount, WtMessage.Status status) {
		return getWtMessages(eq(WT_clmSTATUS), args(status), SortDirection.ASC, maxCount);
	}

	private List<WtMessage> getWtMessages(String selection, String[] selectionArgs, SortDirection sort, int maxCount) {
		Cursor c = null;
		try {
			c = getWtMessageCursor(selection, selectionArgs, sort, maxCount);

			int count = c.getCount();
			log("getWtMessages() :: item fetch count: %s", count);
			ArrayList<WtMessage> list = new ArrayList<>(count);
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

	private Cursor getWtMessageCursor(String selection, String[] selectionArgs, SortDirection sort, int maxCount) {
		return db.query(tblWT_MESSAGE,
				cols(WT_clmID, WT_clmSTATUS, WT_clmLAST_ACTION, WT_clmFROM, WT_clmCONTENT),
				selection, selectionArgs,
				NO_GROUP, NO_GROUP,
				sort == null? null: sort.apply(WT_clmLAST_ACTION),
				Integer.toString(maxCount));
	}

	static WtMessage wtMessageFrom(Cursor c) {
		String id = c.getString(0);
		WtMessage.Status status = WtMessage.Status.valueOf(c.getString(1));
		long lastAction = c.getLong(2);
		String from = c.getString(3);
		String content = c.getString(4);

		return new WtMessage(id, status, lastAction, from, content);
	}

//> DB SEEDING
	private void seed() {
		LogMessages: {
			for(int i=0; i<500000; ++i) {
				storeLogEntry("Seed log entry " + i);
			}
		}

		WtMessages: {
			for(int i=0; i<100000; ++i) {
				store(new WtMessage("+254789123123", "hello from kenya " + i));
				store(new WtMessage("+34678123123", "hello from spain " + i));
				store(new WtMessage("+447890123123", "hello from uk " + i));
			}

			for(int i=0; i<200000; ++i) {
				store(new WtMessage(randomPhoneNumber(), randomSmsContent()));
			}
		}

		WoMessages: {
			for(int i=0; i<100000; ++i) {
				store(new WoMessage(randomUUID().toString(), "+254789123123", "hello kenya " + i));
				store(new WoMessage(randomUUID().toString(), "+34678123123", "hello spain " + i));
				store(new WoMessage(randomUUID().toString(), "+447890123123", "hello uk " + i));
			}

			for(int i=0; i<200000; ++i) {
				store(new WoMessage(randomUUID().toString(), randomPhoneNumber(), randomSmsContent()));
			}
		}
	}

//> STATIC HELPERS
	private static String[] cols(String... args) {
		return args;
	}

	private static String eq(String... cols) { // NOPMD
		StringBuilder bob = new StringBuilder();
		for(String col : cols) {
			bob.append(" AND ")
				.append(col)
				.append("=?");
		}
		return bob.substring(5);
	}

	private void log(String message, Object... extras) {
		trace(this, message, extras);
	}
}

enum SortDirection {
	ASC, DESC;

	public String apply(String column) {
		return column + " " + this.toString();
	}
}
