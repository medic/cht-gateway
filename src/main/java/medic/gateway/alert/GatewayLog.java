package medic.gateway.alert;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteFullException;

import static android.util.Log.d;
import static android.util.Log.i;
import static medic.gateway.alert.BuildConfig.DEBUG;
import static medic.gateway.alert.BuildConfig.LOG_TAG;

public final class GatewayLog {
	private GatewayLog() {}

	public static void logEvent(Context ctx, String message, Object... extras) {
		message = String.format(message, extras);

		i(LOG_TAG, message);
		eventLogEntry(ctx, message);
	}

	public static void trace(Object caller, String message, Object... extras) {
		if(!DEBUG) return;
		message = String.format(message, extras);
		d(LOG_TAG, caller.getClass().getName() + " :: " + message);
	}

	public static void logException(Context ctx, Exception ex, String message, Object... extras) {
		message = String.format(message, extras);

		i(LOG_TAG, message, ex);

		// Do not try to save SQLiteFullException to the database - this
		// will (unsurprisingly) fail if the database is full
		if(!(ex instanceof SQLiteFullException)) {
			eventLogEntry(ctx, message);
		}
	}

	public static void logException(Exception ex, String message, Object... extras) {
		message = String.format(message, extras);

		i(LOG_TAG, message, ex);
	}

	private static void eventLogEntry(Context ctx, String message) {
		try {
			Db.getInstance(ctx).storeLogEntry(message);
		} catch(SQLiteException ex) {
			logException(ctx, ex, "Could not write log entry to DB.");
		}
	}
}
