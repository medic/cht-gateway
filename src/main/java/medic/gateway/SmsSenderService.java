package medic.gateway;

import android.app.*;
import android.content.*;
import android.os.*;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import static medic.gateway.BuildConfig.DEBUG;
import static medic.gateway.DebugLog.logEvent;

public class SmsSenderService extends WakefulIntentService {
	public SmsSenderService() {
		super("SmsSenderService");
	}

	public void doWakefulWork(Intent intent) {
		logEvent(this, "SmsSenderService.onStartCommand()");

		System.err.println("#####################################");
		System.err.println("# SmsSenderService.onStartCommand() #");
		System.err.println("#####################################");

		try {
			sendUnsentSmses();
		} finally {
			SmsSenderService.this.stopSelf();
		}
	}

	private void sendUnsentSmses() {
		for(WoMessage m : WoRepo.$.getUnsent()) {
			try {
				sendSms(m);
				m.status = "pending";
			} catch(Exception ex) {
				if(DEBUG) ex.printStackTrace();
				m.status = "failed";
			} finally {
				WoRepo.$.save(m);
			}
		}
	}

	private void sendSms(WoMessage m) {
		System.err.println("#####################");
		System.err.println("# Sending message: " + m);
		System.err.println("#####################");
	}

	private void log(String message, Object...extras) {
		if(DEBUG) System.err.println("LOG | SmsSenderService :: " +
				String.format(message, extras));
	}
}

