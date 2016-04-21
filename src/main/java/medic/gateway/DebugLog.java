package medic.gateway;

import android.content.*;

import java.text.*;
import java.util.*;

import static medic.gateway.BuildConfig.DEBUG;

public class DebugLog {
	public static void logEvent(Context ctx, String message, Object... messageArgs) {
		message = String.format(message, messageArgs);

		if(DEBUG) {
			System.err.println("###############################");
			System.err.println("# " + message);
			System.err.println("###############################");
		}

		Db.getInstance(ctx).store(new DebugLogEntry(System.currentTimeMillis(), message));
	}
}
