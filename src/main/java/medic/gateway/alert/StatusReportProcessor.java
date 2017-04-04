package medic.gateway.alert;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.telephony.SmsMessage;

import static android.content.Intent.ACTION_BATTERY_CHANGED;
import static medic.gateway.alert.Utils.randomUuid;

@SuppressWarnings({"PMD.ConsecutiveLiteralAppends","PMD.AvoidStringBufferField"})
class StatusReportProcessor implements Runnable {
	private static class StatusReportBuilder {
		private final StringBuilder bob = new StringBuilder(160);
		void add(String key, Object value) {
			bob.append(';'); bob.append(' ');
			bob.append(key); bob.append('='); bob.append(value);
		}
		public String toString() { return bob.length() == 0 ? "" : bob.substring(2); }
	}

	private final Context ctx;
	private final Db db;
	private final SmsMessage trigger;

	StatusReportProcessor(Context ctx, SmsMessage trigger) {
		this.ctx = ctx;
		this.db = Db.getInstance(ctx);
		this.trigger = trigger;
	}

	@Override public void run() {
		StatusReportBuilder report = new StatusReportBuilder();

		addBatteryLevel(report);

		db.store(new WoMessage(randomUuid(), trigger.getOriginatingAddress(), report.toString()));
	}

	private void addBatteryLevel(StatusReportBuilder report) {
		Intent i = ctx.registerReceiver(null, new IntentFilter(ACTION_BATTERY_CHANGED));

		Object val;

		int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

		if(level < 0 || scale <= 0) {
			val = "unknown";
		} else {
			val = Math.round(level * 100.0f / scale);
		}

		report.add("Battery", val);
	}
}
