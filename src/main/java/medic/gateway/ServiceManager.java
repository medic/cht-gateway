package medic.gateway;

import android.app.*;
import android.content.*;
import android.os.*;

import static medic.gateway.BuildConfig.DEBUG;
import static medic.gateway.DebugLog.logEvent;

public class ServiceManager {
	private final Context ctx;

	public ServiceManager(Context ctx) {
		this.ctx = ctx;
	}

	public void run() {
		logEvent(ctx, "ServiceManager.run()");

		ctx.startService(new Intent(ctx, SmsSenderService.class));
		ctx.startService(new Intent(ctx, WebappPoller.class));
	}

//> STATIC API
	public static void restart(Context ctx) {
		stop(ctx);
		start(ctx);
	}

//> STATIC HELPERS
	private static AlarmManager getAm(Context ctx) {
		return (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
	}

	private static void start(Context ctx) {
		logEvent(ctx, "ServiceManager.start()");

		getAm(ctx).setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), getPollInterval(ctx), getIntent(ctx));
	}

	private static void stop(Context ctx) {
		logEvent(ctx, "ServiceManager.stop()");

		getAm(ctx).cancel(getIntent(ctx));
	}

	private static PendingIntent getIntent(Context ctx) {
		Intent alarmIntent = new Intent("medic.gateway.ACTION_POLL");
		return PendingIntent.getBroadcast(ctx, 0, alarmIntent, 0);
	}

	private static long getPollInterval(Context ctx) {
		return SettingsStore.in(ctx).getPollInterval();
	}
}
