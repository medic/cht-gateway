package medic.gateway;

import android.app.*;
import android.content.*;
import android.os.*;

import static medic.gateway.BuildConfig.DEBUG;
import static medic.gateway.DebugLog.logEvent;

public class SmsSender {
	private final Context ctx;

	public SmsSender(Context ctx) {
		this.ctx = ctx;
	}

	public void sendUnsentSmses() {
		for(WoMessage m : WoRepo.$.getUnsent()) {
			try {
				sendSms(m);
				m.status = WoMessage.Status.PENDING;
			} catch(Exception ex) {
				if(DEBUG) ex.printStackTrace();
				m.status = WoMessage.Status.FAILED;
			} finally {
				WoRepo.$.save(m);
			}
		}
	}

	private void sendSms(WoMessage m) {
		if(DEBUG) System.err.println("#####################");
		if(DEBUG) System.err.println("# Sending message: " + m);
		if(DEBUG) System.err.println("#####################");

		logEvent(ctx, "sendSms() :: [" + m.to + "] '" + m.content + "'");
	}

	private void log(String message, Object...extras) {
		if(DEBUG) System.err.println("LOG | SmsSender :: " +
				String.format(message, extras));
	}
}

