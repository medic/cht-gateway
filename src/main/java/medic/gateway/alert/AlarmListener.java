package medic.gateway.alert;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.annotation.SuppressLint;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import static medic.gateway.alert.GatewayLog.logEvent;
import static medic.gateway.alert.Settings.POLL_INTERVAL;

@SuppressLint("ShortAlarm") // On Android 5.1+, poll interval will be forced up to 60s if below that
public class AlarmListener implements WakefulIntentService.AlarmListener {
	public void scheduleAlarms(AlarmManager am, PendingIntent pendingIntent, Context ctx) {
		Settings s = Settings.in(ctx);
		if(s != null && s.pollingEnabled) {
			logEvent(ctx, "AlarmManager.scheduleAlarms() :: polling enabled - setting alarms");

			// On SDK >= 19, setRepeating will be inexact - the OS will try to fit alarms in with other
			// activity which wakes the device.  This should be better for battery life, and -seems-
			// acceptable.  However, testing across a range of devices may prove that it is simpler
			// to use setWindow(), and reschedule the alarm each time it fires.
			am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), POLL_INTERVAL, pendingIntent);
		} else {
			logEvent(ctx, "AlarmManager.scheduleAlarms() :: polling disabled - cancelling alarms");
			WakefulIntentService.cancelAlarms(ctx);
		}
	}

	public void sendWakefulWork(Context ctx) {
		WakefulIntentService.sendWakefulWork(ctx, new Intent(ctx, WakefulService.class));
	}

	public long getMaxAge(Context ctx) {
		return POLL_INTERVAL * 2L;
	}

//> PUBLIC STATIC
	public static void restart(Context ctx) {
		WakefulIntentService.cancelAlarms(ctx);
		WakefulIntentService.scheduleAlarms(new AlarmListener(), ctx);
	}
}
