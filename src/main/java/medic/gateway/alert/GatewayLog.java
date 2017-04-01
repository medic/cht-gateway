package medic.gateway.alert;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteFullException;

import static android.util.Log.d;
import static android.util.Log.i;
import static android.util.Log.w;
import static medic.gateway.alert.BuildConfig.DEBUG;
import static medic.gateway.alert.BuildConfig.LOG_TAG;

public final class GatewayLog {
	private GatewayLog() {}

	public static void logEvent(Context ctx, String message, Object... extras) {
		message = String.format(message, extras);

		i(LOG_TAG, message);
		eventLogEntry(ctx, message);
	}

	public static void warn(String message, Object... extras) {
		message = String.format(message, extras);

		w(LOG_TAG, message);
	}

	public static void warnEvent(Context ctx, String message, Object... extras) {
		message = String.format(message, extras);

		w(LOG_TAG, message);
		eventLogEntry(ctx, "WARNING: " + message);
	}

	public static void trace(Object caller, String message, Object... extras) {
		if(!DEBUG) return;
		message = String.format(message, extras);
		Class callerClass = caller instanceof Class ? (Class) caller : caller.getClass();
		d(LOG_TAG, String.format("%s :: %s", callerClass.getName(), message));
	}

	public static void logException(Context ctx, Exception ex, String message, Object... extras) {
		message = forException(ex, message, extras);

		i(LOG_TAG, message, ex);

		// Do not try to save SQLiteFullException to the database - this
		// will (unsurprisingly) fail if the database is full
		if(!(ex instanceof SQLiteFullException)) {
			eventLogEntry(ctx, message);
		}
	}

	public static void logException(Exception ex, String message, Object... extras) {
		message = forException(ex, message, extras);

		i(LOG_TAG, message, ex);
	}

	public static void warnException(Exception ex, String message, Object... extras) {
		message = String.format(message, extras);

		w(LOG_TAG, message, ex);
	}

	private static void eventLogEntry(Context ctx, String message) {
		try {
			Db.getInstance(ctx).storeLogEntry(message);
		} catch(SQLiteException ex) {
			logException(ex, "Could not write log entry to DB.");
		}
	}

	private static String forException(Exception ex, String message, Object... extras) {
		return String.format("%s :: %s",
				String.format(message, extras),
				ex);
	}
}
