package medic.gateway;

import android.app.*;
import android.content.*;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import static medic.gateway.DebugLog.logEvent;

public class AlarmListener implements WakefulIntentService.AlarmListener {
	public AlarmListener() {}

	public void restart(Context ctx) {
		WakefulIntentService.cancelAlarms(ctx); // this may not be necessary - check docs
		WakefulIntentService.scheduleAlarms(this, ctx);
	}

	public void scheduleAlarms(AlarmManager am, PendingIntent pendingIntent, Context ctx) {
		am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), getPollInterval(ctx), pendingIntent);
	}

	public void sendWakefulWork(Context ctx) {
		logEvent(ctx, "AlarmManager.sendWakefulWork()");
		WakefulIntentService.sendWakefulWork(ctx, new Intent(ctx, WakefulService.class));
	}

	public long getMaxAge() {
		// TODO return poll frequency in ms from config
		return 30000L;
	}

//> STATIC HELPERS
	private static long getPollInterval(Context ctx) {
		return SettingsStore.in(ctx).getPollInterval();
	}
}
