package medic.gateway;

import android.content.*;
import android.database.sqlite.*;
import android.util.*;

import java.text.*;
import java.util.*;

import static android.util.Log.*;
import static medic.gateway.BuildConfig.DEBUG;
import static medic.gateway.BuildConfig.LOG_TAG;

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
		eventLogEntry(ctx, message);
	}

	public static void logException(Exception ex, String message, Object... extras) {
		message = String.format(message, extras);

		d(LOG_TAG, message, ex);
	}

	private static void eventLogEntry(Context ctx, String message) {
		try {
			Db.getInstance(ctx).storeLogEntry(message);
		} catch(SQLiteException ex) {
			logException(ctx, ex, "Could not write log entry to DB.");
		}
	}
}
