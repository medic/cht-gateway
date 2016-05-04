package medic.gateway;

import android.app.*;
import android.content.*;
import android.os.*;
import android.telephony.*;

import java.util.*;

import static android.telephony.PhoneNumberUtils.isGlobalPhoneNumber;
import static medic.gateway.GatewayLog.*;
import static medic.gateway.IntentProcessor.DELIVERY_REPORT;
import static medic.gateway.IntentProcessor.SENDING_REPORT;
import static medic.gateway.Utils.*;
import static medic.gateway.WoMessage.Status.*;

@SuppressWarnings("PMD.LooseCoupling")
public class SmsSender {
	private static final int MAX_WO_MESSAGES = 10;
	private static final String DEFAULT_SMSC = null;

	private final Context ctx;
	private final Db db;
	private final SmsManager smsManager;
	private final Random r;

	public SmsSender(Context ctx) {
		this.ctx = ctx;
		this.db = Db.getInstance(ctx);
		this.smsManager = SmsManager.getDefault();
		this.r = new Random();
	}

	public void sendUnsentSmses() {
		trace(this, "sendUnsentSmses()");
		for(WoMessage m : db.getWoMessages(MAX_WO_MESSAGES, UNSENT)) {
			try {
				trace(this, "sendUnsentSmses() :: attempting to send %s", m);
				sendSms(m);
			} catch(Exception ex) {
				logException(ex, "SmsSender.sendUnsentSmses() :: message=%s", m);
				db.updateStatus(m, PENDING, FAILED);
			}
		}
	}

	private void sendSms(WoMessage m) {
		logEvent(ctx, "sendSms() :: [%s] '%s'", m.to, m.content);

		if(isGlobalPhoneNumber(m.to)) {
			boolean statusUpdated = db.updateStatus(m, UNSENT, PENDING);
			if(statusUpdated) {
				ArrayList<String> parts = smsManager.divideMessage(m.content);
				smsManager.sendMultipartTextMessage(m.to, DEFAULT_SMSC,
						parts,
						intentsFor(SENDING_REPORT, m, parts),
						intentsFor(DELIVERY_REPORT, m, parts));
			}
		} else {
			logEvent(ctx, "Not sending SMS to '%s' because number appears invalid ('%s')",
					m.to, m.content);
			db.updateStatus(m, PENDING, REJECTED);
		}
	}

	private ArrayList<PendingIntent> intentsFor(String intentType, WoMessage m, ArrayList<String> parts) {
		int count = parts.size();
		ArrayList<PendingIntent> intents = new ArrayList<>(count);
		for(int i=0; i<count; ++i) {
			intents.add(pendingIntentFor(intentType, m, i));
		}
		return intents;
	}

	private PendingIntent pendingIntentFor(String intentType, WoMessage m, int partIndex) {
		Intent intent = new Intent(intentType);
		intent.putExtra("id", m.id);
		intent.putExtra("part", partIndex);

		// Use a random number for the PendingIntent's requestCode - we
		// will never want to cancel these intents, and we do not want
		// collisions.  There is a small chance of collisions if two
		// SMS are in-flight at the same time and are given the same id.
		// TODO use an algorithm that's less likely to generate colliding values
		int requestCode = r.nextInt();

		return PendingIntent.getBroadcast(ctx, requestCode, intent, PendingIntent.FLAG_ONE_SHOT);
	}
}
