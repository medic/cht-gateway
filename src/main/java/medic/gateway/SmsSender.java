package medic.gateway;

import android.app.*;
import android.content.*;
import android.os.*;

import static medic.gateway.BuildConfig.DEBUG;
import static medic.gateway.DebugLog.logEvent;

public class SmsSender {
	public void sendUnsentSmses() {
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
		if(DEBUG) System.err.println("LOG | SmsSender :: " +
				String.format(message, extras));
	}
}

