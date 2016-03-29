package medic.gateway;

import android.app.*;
import android.content.*;
import android.os.*;

import static android.app.Service.*;
import static medic.gateway.BuildConfig.DEBUG;

public class SmsSenderService extends Service {
	public int onStartCommand(Intent intent, int flags, int startId) {
		System.err.println("#####################################");
		System.err.println("# SmsSenderService.onStartCommand() #");
		System.err.println("#####################################");

		new Thread() {
			public void run() {
				try {
					sendUnsentSmses();
				} finally {
					SmsSenderService.this.stopSelf();
				}
			}
		}.start();

		return START_STICKY;
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

	public IBinder onBind(Intent _) { return null; }

	private void log(String message, Object...extras) {
		if(DEBUG) System.err.println("LOG | SmsSenderService :: " +
				String.format(message, extras));
	}
}

