package medic.gateway;

import android.app.*;
import android.content.*;
import android.os.*;

import static medic.gateway.BuildConfig.DEBUG;
import static medic.gateway.DebugLog.logEvent;

public class SmsSender {
	private static final int MAX_WO_MESSAGES = 10;

	private final Context ctx;
	private final Db db;

	public SmsSender(Context ctx) {
		this.ctx = ctx;
		this.db = Db.getInstance(ctx);
	}

	public void sendUnsentSmses() {
		for(WoMessage m : db.getWoMessages(MAX_WO_MESSAGES, WoMessage.Status.UNSENT)) {
			try {
				sendSms(m);
				m.setStatus(WoMessage.Status.PENDING);
			} catch(Exception ex) {
				if(DEBUG) ex.printStackTrace();
				m.setStatus(WoMessage.Status.FAILED);
			} finally {
				db.update(m);
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

