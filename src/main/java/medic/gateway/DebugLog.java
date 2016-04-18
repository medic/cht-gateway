package medic.gateway;

import android.content.*;

import java.text.*;
import java.util.*;

public class DebugLog {
	public static void logEvent(Context ctx, String message) {
		Db.getInstance(ctx).store(new DebugLogEntry(System.currentTimeMillis(), message));
	}
}
