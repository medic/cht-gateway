package medic.gateway.alert;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.database.Cursor;
import android.database.SQLException;
import android.telephony.SmsMessage;

import java.util.ArrayList;
import java.util.List;

import static java.util.UUID.randomUUID;
import static medic.gateway.alert.BuildConfig.DEBUG;
import static medic.gateway.alert.BuildConfig.FORCE_SEED;
import static medic.gateway.alert.BuildConfig.LOAD_SEED_DATA;
import static medic.gateway.alert.GatewayLog.logEvent;
import static medic.gateway.alert.GatewayLog.trace;
import static medic.gateway.alert.GatewayLog.warnException;
import static medic.gateway.alert.Utils.args;
import static medic.gateway.alert.DebugUtils.randomPhoneNumber;
import static medic.gateway.alert.DebugUtils.randomSmsContent;

@SuppressWarnings({"PMD.GodClass", "PMD.TooManyMethods"})
public final class Db extends SQLiteOpenHelper {
	private static final int SCHEMA_VERSION = 5;

	private static final String ALL = null, NO_GROUP = null;
	private static final String[] NO_ARGS = {};
	private static final String NO_CRITERIA = null;
	private static final String NO_LIMIT = null;
	private static final String DEFAULT_SORT_ORDER = null;

	private static final String tblLOG = "log";
	private static final String LOG_clmID = "_id";
	private static final String LOG_clmTIMESTAMP = "timestamp";
	private static final String LOG_clmMESSAGE = "message";

	private static final String tblWT_MESSAGE = "wt_message";
	private static final String WTM_clmID = "_id";
	private static final String WTM_clmSTATUS = "status";
	private static final String WTM_clmLAST_ACTION = "last_action";
	private static final String WTM_clmFROM = "_from";
	private static final String WTM_clmCONTENT = "content";
	private static final String WTM_clmSMS_SENT = "sms_sent";
	private static final String WTM_clmSMS_RECEIVED = "sms_received";

	private static final String tblWT_STATUS = "wtm_status";
	private static final String WTS_clmID = "_id";
	private static final String WTS_clmMESSAGE_ID = "message_id";
	private static final String WTS_clmSTATUS = "status";
	private static final String WTS_clmTIMESTAMP = "timestamp";

	private static final String tblWO_MESSAGE = "wo_message";
	private static final String WOM_clmID = "_id";
	private static final String WOM_clmSTATUS = "status";
	private static final String WOM_clmSTATUS_NEEDS_FORWARDING = "status_needs_forwarding";
	private static final String WOM_clmFAILURE_REASON = "failure_reason";
	private static final String WOM_clmLAST_ACTION = "last_action";
	private static final String WOM_clmTO = "_to";
	private static final String WOM_clmCONTENT = "content";

	private static final String tblWO_STATUS = "wom_status";
	private static final String WOS_clmID = "_id";
	private static final String WOS_clmMESSAGE_ID = "message_id";
	private static final String WOS_clmSTATUS = "status";
	private static final String WOS_clmFAILURE_REASON = "failure_reason";
	private static final String WOS_clmTIMESTAMP = "timestamp";
	private static final String WOS_clmNEEDS_FORWARDING = "needs_forwarding";
	private static final String[] WOS_SELECT_COLS = new String[] {
		WOS_clmID, WOS_clmMESSAGE_ID, WOS_clmSTATUS, WOS_clmFAILURE_REASON, WOS_clmTIMESTAMP };

	private static final String TRUE  = "1";
	private static final String FALSE = "0";

	private static Db _instance;

	private final Context ctx;
	private final SQLiteDatabase db; // NOPMD

	/** a soft limit for the number of log entries to store in the system */
	private int logEntryLimit;
	private String logEntryLimitString;

	public static synchronized Db getInstance(Context ctx) { // NOPMD
		if(_instance == null) {
			_instance = new Db(ctx);
			if(LOAD_SEED_DATA && (FORCE_SEED ||
					_instance.db.compileStatement("SELECT COUNT(*) FROM " + tblLOG).simpleQueryForLong() == 0)) {
				_instance.seed();
			}

			if(DEBUG) _instance.storeLogEntry("Log entries: " + _instance.db.compileStatement("SELECT COUNT(*) FROM " + tblLOG).simpleQueryForLong());
			if(DEBUG) _instance.storeLogEntry("WT messages: " + _instance.db.compileStatement("SELECT COUNT(*) FROM " + tblWT_MESSAGE).simpleQueryForLong());
			if(DEBUG) _instance.storeLogEntry("WT message status updates: " + _instance.db.compileStatement("SELECT COUNT(*) FROM " + tblWT_STATUS).simpleQueryForLong());
			if(DEBUG) _instance.storeLogEntry("WO messages: " + _instance.db.compileStatement("SELECT COUNT(*) FROM " + tblWO_MESSAGE).simpleQueryForLong());
			if(DEBUG) _instance.storeLogEntry("WO message status updates: " + _instance.db.compileStatement("SELECT COUNT(*) FROM " + tblWO_STATUS).simpleQueryForLong());
		}

		return _instance;
	}

	private Db(Context ctx) {
		super(ctx, "medic_gateway", null, SCHEMA_VERSION);
		this.ctx = ctx;
		db = getWritableDatabase();

		setLogEntryLimit(200);
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
					"%s TEXT NOT NULL, " +
					"%s INTEGER NOT NULL, " +
					"%s INTEGER NOT NULL)",
				tblWT_MESSAGE, WTM_clmID, WTM_clmSTATUS, WTM_clmLAST_ACTION, WTM_clmFROM, WTM_clmCONTENT, WTM_clmSMS_SENT, WTM_clmSMS_RECEIVED));

		db.execSQL(String.format("CREATE TABLE %s (" +
					"%s TEXT NOT NULL PRIMARY KEY, " +
					"%s TEXT NOT NULL, " +
					"%s TEXT, " +
					"%s INTEGER NOT NULL, " +
					"%s TEXT NOT NULL, " +
					"%s TEXT NOT NULL)",
				tblWO_MESSAGE, WOM_clmID, WOM_clmSTATUS, WOM_clmFAILURE_REASON, WOM_clmLAST_ACTION, WOM_clmTO, WOM_clmCONTENT));

		migrate_createTable_WoMessageStatusUpdate(db, true);
		migrate_createTable_WtMessageStatusUpdate(db, true);
	}

	public void onUpgrade(SQLiteDatabase db,
			int oldVersion,
			int newVersion) {
		trace(this, "onUpgrade() :: oldVersion=%s, newVersion=%s", oldVersion, newVersion);
		if(oldVersion < 2) {
			migrate_createTable_WoMessageStatusUpdate(db, false);
		}
		if(oldVersion < 3) {
			migrate_create_WOS_clmNEEDS_FORWARDING(db);
		}
		if(oldVersion < 4) {
			migrate_createTable_WtMessageStatusUpdate(db, false);
		}
		if(oldVersion < 5) {
			migrate_create_WTM_clmSMS_SENT__clmSMS_RECEIVED(db);
		}
	}

//> MIGRATIONS
	static void migrate_createTable_WoMessageStatusUpdate(SQLiteDatabase db, boolean isCleanDb) {
		trace(db, "onUpgrade() :: migrate_createTable_WoMessageStatusUpdate()");
		db.execSQL(String.format("CREATE TABLE %s (" +
					"%s INTEGER PRIMARY KEY, " +
					"%s TEXT NOT NULL, " +
					"%s TEXT NOT NULL, " +
					"%s TEXT, " +
					"%s INTEGER NOT NULL, " +
					"%s INTEGER NOT NULL)",
				tblWO_STATUS, WOS_clmID, WOS_clmMESSAGE_ID, WOS_clmSTATUS, WOS_clmFAILURE_REASON, WOS_clmTIMESTAMP, WOS_clmNEEDS_FORWARDING));

		if(!isCleanDb) {
			db.execSQL(String.format("INSERT INTO %s(%s, %s, %s, %s, %s) SELECT %s, %s, %s, %s, %s FROM %s",
					tblWO_STATUS, WOS_clmMESSAGE_ID, WOS_clmSTATUS, WOS_clmFAILURE_REASON, WOS_clmTIMESTAMP, WOS_clmNEEDS_FORWARDING,
							WOM_clmID, WOM_clmSTATUS, WOM_clmFAILURE_REASON, WOM_clmLAST_ACTION, WOM_clmSTATUS_NEEDS_FORWARDING, tblWO_MESSAGE));
		}
	}

	static void migrate_createTable_WtMessageStatusUpdate(SQLiteDatabase db, boolean isCleanDb) {
		trace(db, "onUpgrade() :: migrate_createTable_WtMessageStatusUpdate()");
		db.execSQL(String.format("CREATE TABLE %s (" +
					"%s INTEGER PRIMARY KEY, " +
					"%s TEXT NOT NULL, " +
					"%s TEXT NOT NULL, " +
					"%s INTEGER NOT NULL)",
				tblWT_STATUS, WTS_clmID, WTS_clmMESSAGE_ID, WTS_clmSTATUS, WTS_clmTIMESTAMP));

		if(!isCleanDb) {
			db.execSQL(String.format("INSERT INTO %s(%s, %s, %s) SELECT %s, %s, %s FROM %s",
					tblWT_STATUS, WTS_clmMESSAGE_ID, WTS_clmSTATUS, WTS_clmTIMESTAMP,
							WTM_clmID, WTM_clmSTATUS, WTM_clmLAST_ACTION, tblWT_MESSAGE));
		}
	}

	static void migrate_create_WOS_clmNEEDS_FORWARDING(SQLiteDatabase db) {
		trace(db, "onUpgrade() :: migrate_create_WOS_clmNEEDS_FORWARDING()");
		db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s INTEGER NOT NULL DEFAULT(0)",
				tblWO_STATUS, WOS_clmNEEDS_FORWARDING));
		// copy need_forwarding column values from wo_message into wom_status
		rawUpdateOrDelete(db, "UPDATE %s SET %s=1 WHERE (%s || '_' || %s || '_' || %s) " +
						"IN(SELECT  (%s || '_' || %s || '_' || %s) FROM %s WHERE %s=1)",
				cols(tblWO_STATUS, WOS_clmNEEDS_FORWARDING, WOS_clmMESSAGE_ID, WOS_clmSTATUS, WOS_clmTIMESTAMP,
						WOM_clmID, WOM_clmSTATUS, WOM_clmLAST_ACTION, tblWO_MESSAGE, WOM_clmSTATUS_NEEDS_FORWARDING));

		// We should now drop the status_needs_forwarding column from
		// the wo_message table.  However, dropping columns is not
		// directly supported in SQLite.  There seems little harm in
		// leaving the column in place.
	}

	static void migrate_create_WTM_clmSMS_SENT__clmSMS_RECEIVED(SQLiteDatabase db) {
		trace(db, "onUpgrade() :: migrate_create_WTM_clmSMS_SENT__clmSMS_RECEIVED()");
		db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s INTEGER NOT NULL DEFAULT(0)",
				tblWT_MESSAGE, WTM_clmSMS_SENT));
		db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s INTEGER NOT NULL DEFAULT(0)",
				tblWT_MESSAGE, WTM_clmSMS_RECEIVED));

		// These values were not stored for old messages, so we can't
		// set a meaningful value for these columns for old messages.
	}

//> ACCESSORS
	void setLogEntryLimit(int limit) {
		logEntryLimit = limit;
		logEntryLimitString = Integer.toString(limit);
	}

//> GENERAL HANDLERS
	int deleteOldData() {
		long oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);

		int totalRecordsDeleted = 0;

		totalRecordsDeleted += db.delete(tblLOG, lt(LOG_clmTIMESTAMP), args(oneWeekAgo));
		totalRecordsDeleted += db.delete(tblWO_MESSAGE, lt(WOM_clmLAST_ACTION), args(oneWeekAgo));
		totalRecordsDeleted += db.delete(tblWT_MESSAGE, lt(WTM_clmLAST_ACTION), args(oneWeekAgo));

		// TODO do we need to VACUUM after deleting?

		return totalRecordsDeleted;
	}

//> GatewayEventLogEntry HANDLERS
	void storeLogEntry(String message) {
		ContentValues v = new ContentValues();
		v.put(LOG_clmTIMESTAMP, System.currentTimeMillis());
		v.put(LOG_clmMESSAGE, message);

		try {
			db.insertOrThrow(tblLOG, null, v);
		} catch(SQLException ex) {
			warnException(ex, "Exception writing log entry to db: %s", message);
		}
	}

	Cursor getLogEntries() {
		return db.query(tblLOG,
				cols(LOG_clmID, LOG_clmTIMESTAMP, LOG_clmMESSAGE),
				ALL, NO_ARGS,
				NO_GROUP, NO_GROUP,
				SortDirection.DESC.apply(LOG_clmID),
				logEntryLimitString);
	}

	void cleanLogs() {
		rawUpdateOrDelete("DELETE FROM %s WHERE %s < (SELECT %s FROM %s LIMIT (SELECT (COUNT(*) - ?) FROM %s),1)",
				cols(tblLOG, LOG_clmID, LOG_clmID, tblLOG, tblLOG),
				args(logEntryLimit));
	}

//> WoMessage HANDLERS
	boolean store(WoMessage m) {
		log("store() :: %s", m);
		try {
			long id = db.insertOrThrow(tblWO_MESSAGE, null, getContentValues(m));

			if(id != -1) {
				storeStatusUpdate(m, m.status, null, m.lastAction, false);
				return true;
			} else {
				return false;
			}
		} catch(SQLiteConstraintException ex) {
			// Likely this is because a message with this ID already exists.  If so,
			// we should update that message so that its status is synched with the
			// server.  This should stop the server from re-sending the same message
			// repeatedly.
			logEvent(ctx, "Message %s appears to be in database already; will be updated.", m);
			return touch(m);
		} catch(SQLException ex) {
			warnException(ex, "Exception writing WoMessage to db: %s", m);
			return false;
		}
	}

	void setFailed(WoMessage m, String failureReason) {
		updateStatus(m, WoMessage.Status.PENDING, WoMessage.Status.FAILED, failureReason);
	}

	boolean updateStatus(WoMessage m, WoMessage.Status newStatus) {
		return updateStatus(m, m.status, newStatus);
	}

	boolean updateStatus(WoMessage m, WoMessage.Status oldStatus, WoMessage.Status newStatus) {
		if(newStatus == WoMessage.Status.FAILED)
			throw new IllegalArgumentException("updateStatus() should not be called with newStatus==FAILED.  Use setFailed().");

		return updateStatus(m, oldStatus, newStatus, null);
	}

	private boolean updateStatus(WoMessage m, WoMessage.Status oldStatus, WoMessage.Status newStatus, String failureReason) {
		log("updateStatus() :: %s :: %s -> %s (%s)", m, oldStatus, newStatus, failureReason);

		if((newStatus == WoMessage.Status.FAILED) == (failureReason == null))
			throw new IllegalArgumentException(String.format(
					"Give failureReason iff new status == FAILED (newStatus=%s, failureReason=%s)",
					newStatus,
					failureReason));

		long timestamp = System.currentTimeMillis();

		ContentValues v = new ContentValues();
		v.put(WOM_clmSTATUS, newStatus.toString());
		v.put(WOM_clmFAILURE_REASON, failureReason);
		v.put(WOM_clmLAST_ACTION, timestamp);

		int affected;
		if(oldStatus == null) {
			affected = db.update(tblWO_MESSAGE, v, eq(WOM_clmID), args(m.id));
		} else {
			affected = db.update(tblWO_MESSAGE, v, eq(WOM_clmID, WOM_clmSTATUS), args(m.id, oldStatus));
		}

		if(affected > 0) {
			storeStatusUpdate(m, newStatus, failureReason, timestamp, true);
			return true;
		} else {
			return false;
		}
	}

	void setStatusForwarded(WoMessage.StatusUpdate u) {
		log("setStatusForwarded() :: %s", u);

		ContentValues v = new ContentValues();
		v.put(WOS_clmNEEDS_FORWARDING, FALSE);

		db.update(tblWO_STATUS, v, eq(WOS_clmID, WOS_clmSTATUS), args(u.id, u.newStatus));
	}

	private boolean touch(WoMessage m) {
		log("touch() :: %s", m);

		ContentValues suV = new ContentValues();
		suV.put(WOS_clmNEEDS_FORWARDING, TRUE);

		Cursor c = null;
		try {
			int affectedRows = rawUpdateOrDelete("UPDATE %s SET %s=? WHERE %s IN (SELECT %s FROM %s WHERE %s=? ORDER BY %s DESC LIMIT 1)",
					cols(tblWO_STATUS, WOS_clmNEEDS_FORWARDING, WOS_clmID, WOS_clmID, tblWO_STATUS, WOS_clmMESSAGE_ID, WOS_clmID),
					args(TRUE, m.id));
			if(affectedRows > 0) {
				ContentValues mV = new ContentValues();
				mV.put(WOM_clmLAST_ACTION, System.currentTimeMillis());
				db.update(tblWO_MESSAGE, mV, eq(WOM_clmID), args(m.id));

				return true;
			} else return false;
		} finally {
			if(c != null) c.close();
		}
	}

	private void storeStatusUpdate(WoMessage m, WoMessage.Status newStatus, String failureReason, long timestamp, boolean needsForwarding) {
		try {
			db.insertOrThrow(tblWO_STATUS, null, getContentValues(m, newStatus, failureReason, timestamp, needsForwarding));
		} catch(SQLException ex) {
			warnException(ex, "Exception writing WO StatusUpdate [%s] to db for WoMessage: %s", newStatus, m);
		}
	}

	private ContentValues getContentValues(WoMessage m) {
		ContentValues v = new ContentValues();
		v.put(WOM_clmID, m.id);
		v.put(WOM_clmSTATUS, m.status.toString());
		v.put(WOM_clmFAILURE_REASON, m.status == WoMessage.Status.FAILED ? m.getFailureReason() : null);
		v.put(WOM_clmLAST_ACTION, System.currentTimeMillis());
		v.put(WOM_clmTO, m.to);
		v.put(WOM_clmCONTENT, m.content);
		return v;
	}

	private ContentValues getContentValues(WoMessage m, WoMessage.Status newStatus, String failureReason, long timestamp, boolean needsForwarding) {
		ContentValues v = new ContentValues();
		v.put(WOS_clmMESSAGE_ID, m.id);
		v.put(WOS_clmSTATUS, newStatus.toString());
		v.put(WOS_clmFAILURE_REASON, failureReason);
		v.put(WOS_clmTIMESTAMP, timestamp);
		v.put(WOS_clmNEEDS_FORWARDING, bool(needsForwarding));
		return v;
	}

	WoMessage getWoMessage(String id) {
		List<WoMessage> matches = getWoMessages(eq(WOM_clmID), args(id), null, 1);
		if(matches.isEmpty()) return null;
		return matches.get(0);
	}

	Cursor getWoMessages(int maxCount) {
		return getWoMessageCursor(null, null, SortDirection.DESC, maxCount);
	}

	List<WoMessage> getWoMessages(int maxCount, WoMessage.Status status) {
		return getWoMessages(eq(WOM_clmSTATUS), args(status), SortDirection.ASC, maxCount);
	}

	List<WoMessage.StatusUpdate> getWoMessageStatusUpdates(int maxCount) {
		Cursor c = null;
		try {
			c = db.query(tblWO_STATUS,
					WOS_SELECT_COLS,
					eq(WOS_clmNEEDS_FORWARDING), args(TRUE),
					NO_GROUP, NO_GROUP,
					DEFAULT_SORT_ORDER,
					Integer.toString(maxCount));

			int count = c.getCount();
			log("getWoMessageStatusUpdates() :: item fetch count: %s", count);
			ArrayList<WoMessage.StatusUpdate> list = new ArrayList<>(count);
			c.moveToFirst();
			while(count-- > 0) {
				list.add(woMessageStatusUpdateFrom(c));
				c.moveToNext();
			}
			return list;
		} finally {
			if(c != null) c.close();
		}
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
				cols(WOM_clmID, WOM_clmSTATUS, WOM_clmFAILURE_REASON, WOM_clmLAST_ACTION, WOM_clmTO, WOM_clmCONTENT),
				selection, selectionArgs,
				NO_GROUP, NO_GROUP,
				sort == null? null: sort.apply(WOM_clmLAST_ACTION),
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

	private static WoMessage.StatusUpdate woMessageStatusUpdateFrom(Cursor c) {
		long id = c.getLong(0);
		String messageId = c.getString(1);
		WoMessage.Status status = WoMessage.Status.valueOf(c.getString(2));
		String failureReason = c.getString(3);
		long timestamp = c.getLong(4);

		return new WoMessage.StatusUpdate(id, messageId, status, failureReason, timestamp);
	}

	public List<WoMessage.StatusUpdate> getStatusUpdates(WoMessage m) {
		Cursor c = null;
		try {
			c = db.query(tblWO_STATUS,
					WOS_SELECT_COLS,
					eq(WOS_clmMESSAGE_ID), args(m.id),
					NO_GROUP, NO_GROUP,
					DEFAULT_SORT_ORDER,
					NO_LIMIT);

			int count = c.getCount();
			log("getStatusUpdates(WoMessage) :: item fetch count: %s", count);
			ArrayList<WoMessage.StatusUpdate> list = new ArrayList<>(count);
			c.moveToFirst();
			while(count-- > 0) {
				list.add(woMessageStatusUpdateFrom(c));
				c.moveToNext();
			}
			return list;
		} finally {
			if(c != null) c.close();
		}
	}

//> WtMessage HANDLERS
	boolean store(MultipartSms sms) {
		WtMessage m = new WtMessage(
				sms.getOriginatingAddress(),
				sms.getMessageBody(),
				sms.getTimestampMillis());
		return store(m);
	}

	boolean store(SmsMessage sms) {
		WtMessage m = new WtMessage(
				sms.getOriginatingAddress(),
				sms.getMessageBody(),
				sms.getTimestampMillis());
		return store(m);
	}

	boolean store(WtMessage m) {
		log("store() :: %s", m);
		try {
			long id = db.insertOrThrow(tblWT_MESSAGE, null, getContentValues(m));

			if(id != -1) {
				storeStatusUpdate(m, m.getStatus(), m.getLastAction());
				return true;
			} else {
				return false;
			}
		} catch(SQLException ex) {
			warnException(ex, "Exception writing WtMessage to db: %s", m);
			return false;
		}
	}

	void updateStatusFrom(WtMessage.Status oldStatus, WtMessage m) {
		WtMessage.Status newStatus = m.getStatus();
		log("updateStatusFrom() :: %s :: %s -> %s", m, oldStatus, newStatus);

		long timestamp = System.currentTimeMillis();

		ContentValues v = new ContentValues();
		v.put(WTM_clmSTATUS, newStatus.toString());
		v.put(WTM_clmLAST_ACTION, m.getLastAction());

		int affected;
		if(oldStatus == null) {
			affected = db.update(tblWT_MESSAGE, v, eq(WTM_clmID), args(m.id));
		} else {
			affected = db.update(tblWT_MESSAGE, v, eq(WTM_clmID, WTM_clmSTATUS), args(m.id, oldStatus));
		}

		if(affected > 0) {
			storeStatusUpdate(m, newStatus, timestamp);
		}
	}

	private void storeStatusUpdate(WtMessage m, WtMessage.Status newStatus, long timestamp) {
		try {
			db.insertOrThrow(tblWT_STATUS, null, getContentValues(m, newStatus, timestamp));
		} catch(SQLException ex) {
			warnException(ex, "Exception writing WT StatusUpdate [%s] to db for WtMessage: %s", newStatus, m);
		}
	}

	private ContentValues getContentValues(WtMessage m, WtMessage.Status newStatus, long timestamp) {
		ContentValues v = new ContentValues();
		v.put(WTS_clmMESSAGE_ID, m.id);
		v.put(WTS_clmSTATUS, newStatus.toString());
		v.put(WTS_clmTIMESTAMP, timestamp);
		return v;
	}

	private ContentValues getContentValues(WtMessage m) {
		ContentValues v = new ContentValues();
		v.put(WTM_clmID, m.id);
		v.put(WTM_clmSTATUS, m.getStatus().toString());
		v.put(WTM_clmLAST_ACTION, m.getLastAction());
		v.put(WTM_clmFROM, m.from);
		v.put(WTM_clmCONTENT, m.content);
		v.put(WTM_clmSMS_SENT, m.smsSent);
		v.put(WTM_clmSMS_RECEIVED, m.smsReceived);
		return v;
	}

	WtMessage getWtMessage(String id) {
		List<WtMessage> matches = getWtMessages(eq(WOM_clmID), args(id), null, 1);
		if(matches.isEmpty()) return null;
		return matches.get(0);
	}

	Cursor getWtMessages(int maxCount) {
		return getWtMessageCursor(NO_CRITERIA, NO_ARGS, SortDirection.DESC, maxCount);
	}

	List<WtMessage> getWtMessages(int maxCount, WtMessage.Status status) {
		return getWtMessages(eq(WTM_clmSTATUS), args(status), SortDirection.ASC, maxCount);
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
						c.getString(4),
						c.getLong(5),
						c.getLong(6)));
				c.moveToNext();
			}
			return list;
		} finally {
			if(c != null) c.close();
		}
	}

	private Cursor getWtMessageCursor(String selection, String[] selectionArgs, SortDirection sort, int maxCount) {
		return db.query(tblWT_MESSAGE,
				cols(WTM_clmID, WTM_clmSTATUS, WTM_clmLAST_ACTION, WTM_clmFROM, WTM_clmCONTENT, WTM_clmSMS_SENT, WTM_clmSMS_RECEIVED),
				selection, selectionArgs,
				NO_GROUP, NO_GROUP,
				sort == null? DEFAULT_SORT_ORDER: sort.apply(WTM_clmLAST_ACTION),
				Integer.toString(maxCount));
	}

	static WtMessage wtMessageFrom(Cursor c) {
		String id = c.getString(0);
		WtMessage.Status status = WtMessage.Status.valueOf(c.getString(1));
		long lastAction = c.getLong(2);
		String from = c.getString(3);
		String content = c.getString(4);
		long smsSent = c.getLong(5);
		long smsReceived = c.getLong(6);

		return new WtMessage(id, status, lastAction, from, content, smsSent, smsReceived);
	}

	private static WtMessage.StatusUpdate wtMessageStatusUpdateFrom(Cursor c) {
		long id = c.getLong(0);
		String messageId = c.getString(1);
		WtMessage.Status status = WtMessage.Status.valueOf(c.getString(2));
		long timestamp = c.getLong(3);

		return new WtMessage.StatusUpdate(id, messageId, status, timestamp);
	}

	public List<WtMessage.StatusUpdate> getStatusUpdates(WtMessage m) {
		Cursor c = null;
		try {
			c = db.query(tblWT_STATUS,
					cols(WTS_clmID, WTS_clmMESSAGE_ID, WTS_clmSTATUS, WTS_clmTIMESTAMP),
					eq(WTS_clmMESSAGE_ID), args(m.id),
					NO_GROUP, NO_GROUP,
					DEFAULT_SORT_ORDER,
					NO_LIMIT);

			int count = c.getCount();
			log("getStatusUpdates(WtMessage) :: item fetch count: %s", count);
			ArrayList<WtMessage.StatusUpdate> list = new ArrayList<>(count);
			c.moveToFirst();
			while(count-- > 0) {
				list.add(wtMessageStatusUpdateFrom(c));
				c.moveToNext();
			}
			return list;
		} finally {
			if(c != null) c.close();
		}
	}

//> DB SEEDING
	private void seed() {
		LogMessages: {
			for(int i=0; i<50; ++i) {
				storeLogEntry("Seed log entry " + i);
			}
		}

		WtMessages: {
			for(int i=0; i<10; ++i) {
				store(new WtMessage("+254789123123", "hello from kenya " + i, i * 3600L * 24L));
				store(new WtMessage("+34678123123", "hello from spain " + i, i * 3600L * 24L));
				store(new WtMessage("+447890123123", "hello from uk " + i, i * 3600L * 24L));
			}

			for(int i=0; i<20; ++i) {
				store(new WtMessage(randomPhoneNumber(), randomSmsContent(), 0));
			}
		}

		WoMessages: {
			for(int i=0; i<10; ++i) {
				store(new WoMessage(randomUUID().toString(), "+254789123123", "hello kenya " + i));
				store(new WoMessage(randomUUID().toString(), "+34678123123", "hello spain " + i));
				store(new WoMessage(randomUUID().toString(), "+447890123123", "hello uk " + i));
			}

			for(int i=0; i<20; ++i) {
				store(new WoMessage(randomUUID().toString(), randomPhoneNumber(), randomSmsContent()));
			}
		}
	}

	private int rawUpdateOrDelete(String statement, String[] cols, String... args) {
		return rawUpdateOrDelete(db, statement, cols, args);
	}

//> STATIC HELPERS
	private static String[] cols(String... args) {
		return args;
	}

	private static String lt(String col) { // NOPMD
		return col + "<?";
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

	private String bool(boolean value) {
		return value ? TRUE : FALSE;
	}

	private void log(String message, Object... extras) {
		trace(this, message, extras);
	}

	@SuppressWarnings("PMD.UnusedPrivateMethod") // it's used - PMD bug?
	private static int rawUpdateOrDelete(SQLiteDatabase db, String statement, String[] cols, String... args) {
		statement = String.format(statement, (String[]) cols);
		SQLiteStatement s = db.compileStatement(statement);
		for(int i=args.length; i>0; --i)
			s.bindString(i, args[i-1]);
		return s.executeUpdateDelete();
	}
}

enum SortDirection {
	ASC, DESC;

	public String apply(String column) {
		return column + " " + this.toString();
	}
}
