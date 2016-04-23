package medic.gateway;

import android.app.*;
import android.content.*;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import static medic.gateway.GatewayLog.*;

public class AlarmListener implements WakefulIntentService.AlarmListener {
	public void scheduleAlarms(AlarmManager am, PendingIntent pendingIntent, Context ctx) {
		if(SettingsStore.in(ctx).isPollingEnabled()) {
			logEvent(ctx, "AlarmManager.scheduleAlarms() :: polling enabled - setting alarms");
			am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), getPollInterval(ctx), pendingIntent);
		} else {
			logEvent(ctx, "AlarmManager.scheduleAlarms() :: polling disabled - cancelling alarms");
			WakefulIntentService.cancelAlarms(ctx);
		}
	}

	public void sendWakefulWork(Context ctx) {
		logEvent(ctx, "AlarmManager.sendWakefulWork()");
		WakefulIntentService.sendWakefulWork(ctx, new Intent(ctx, WakefulService.class));
	}

	public long getMaxAge(Context ctx) {
		// TODO return poll frequency in ms from config, doubled
		return 60000L;
	}

//> PUBLIC STATIC
	public static void restart(Context ctx) {
		WakefulIntentService.cancelAlarms(ctx);
		WakefulIntentService.scheduleAlarms(new AlarmListener(), ctx);
	}

//> STATIC HELPERS
	private static long getPollInterval(Context ctx) {
		return SettingsStore.in(ctx).getPollInterval();
	}
}
