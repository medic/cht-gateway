package medic.gateway.alert;

import android.content.Intent;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import static medic.gateway.alert.GatewayLog.*;

public class WakefulService extends WakefulIntentService {
	public WakefulService() {
		super("WakefulService");
	}

	public void doWakefulWork(Intent intent) {
		try {
			Db.getInstance(this).cleanLogs();
		} catch(Exception ex) {
			logException(this, ex, "Exception caught trying to clean up event log: %s", ex.getMessage());
		}

		try {
			new WebappPoller(this).pollWebapp();
		} catch(Exception ex) {
			logException(this, ex, "Exception caught trying to poll webapp: %s", ex.getMessage());
		}

		try {
			new SmsSender(this).sendUnsentSmses();
		} catch(Exception ex) {
			logException(this, ex, "Exception caught trying to send SMSes: %s", ex.getMessage());
		}
	}
}
