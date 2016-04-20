package medic.gateway;

import android.app.*;
import android.content.*;
import android.os.*;
import android.telephony.*;

import static android.telephony.PhoneNumberUtils.isGlobalPhoneNumber;
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

		if(isGlobalPhoneNumber(m.to)) {
			m.setStatus(WoMessage.Status.PENDING);
		} else {
			logEvent(ctx, "Not sending SMS to '%s' because number appears invalid ('%s')",
					m.to, m.content);
			m.setStatus(WoMessage.Status.REJECTED);
		}
	}

	private void log(String message, Object...extras) {
		if(DEBUG) System.err.println("LOG | SmsSender :: " +
				String.format(message, extras));
	}
}
