package medic.gateway.alert;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;

import java.util.ArrayList;
import java.util.List;

import static android.telephony.PhoneNumberUtils.isGlobalPhoneNumber;
import static medic.gateway.alert.GatewayLog.logEvent;
import static medic.gateway.alert.GatewayLog.logException;
import static medic.gateway.alert.GatewayLog.trace;
import static medic.gateway.alert.IntentProcessor.DELIVERY_REPORT;
import static medic.gateway.alert.IntentProcessor.SENDING_REPORT;
import static medic.gateway.alert.WoMessage.Status.DELIVERED;
import static medic.gateway.alert.WoMessage.Status.PENDING;
import static medic.gateway.alert.WoMessage.Status.SENT;
import static medic.gateway.alert.WoMessage.Status.UNSENT;

@SuppressWarnings("PMD.LooseCoupling")
public class SmsSender {
	private static final int MAX_WO_MESSAGES = 10;
	private static final String DEFAULT_SMSC = null;

	private final Context ctx;
	private final Db db;
	private final SmsManager smsManager;

	/**
	 * Some CDMA networks do not support multipart SMS properly.  On these
	 * networks, we just divide the messages ourselves and send them as
	 * multiple individual messages.
	 * {@code true} if the user has enabled CDMA Compatibility Mode in settings.
	 */
	private final boolean cdmaCompatMode;

	/**
	 * To aid testing of systems dealing with large numbers of messages, you
	 * can enable "dummy send" mode, which will immediately set all outgoing
	 * messages as SENT, instead of actually sending them.
	 */
	private final boolean dummySendMode;

	public SmsSender(Context ctx) {
		this.ctx = ctx;
		this.db = Db.getInstance(ctx);
		this.smsManager = SmsManager.getDefault();

		Settings settings = Settings.in(ctx);
		this.cdmaCompatMode = settings == null ? false : settings.cdmaCompatMode;
		this.dummySendMode = settings == null ? false : settings.dummySendMode;
	}

	public void sendUnsentSmses() {
		List<WoMessage> smsForSending = this.getUnsentMessages();

		if(smsForSending.isEmpty()) {
			logEvent(ctx, "No SMS waiting to be sent.");
		} else {
			logEvent(ctx, "Sending %d SMSs...", smsForSending.size());

			for(WoMessage m : smsForSending) {
				try {
					trace(this, "sendUnsentSmses() :: attempting to send %s", m);
					if(dummySendMode) sendSms_dummy(m);
					else sendSms(m);
				} catch(Exception ex) {
					logException(ex, "SmsSender.sendUnsentSmses() :: message=%s", m);
					db.setFailed(m, String.format("Exception: %s; message: %s; cause: %s",
							ex, ex.getMessage(), ex.getCause()));
				}
			}
		}
	}

	private void sendSms(WoMessage m) {
		logEvent(ctx, "sendSms() :: [%s] '%s'", m.to, m.content);

		boolean statusUpdated = db.updateStatus(m, UNSENT, PENDING);
		if(statusUpdated) {
			if(isGlobalPhoneNumber(m.to)) {
				if(cdmaCompatMode) {
					ArrayList<String> parts = divideMessageForCdma(m.content);
					int totalParts = parts.size();
					for(int partIndex=0; partIndex<totalParts; ++partIndex) {
						String part = parts.get(partIndex);
						smsManager.sendTextMessage(
								m.to,
								DEFAULT_SMSC,
								part,
								intentFor(SENDING_REPORT, m, partIndex, totalParts),
								intentFor(DELIVERY_REPORT, m, partIndex, totalParts));
					}
				} else {
					ArrayList<String> parts = smsManager.divideMessage(m.content);
					smsManager.sendMultipartTextMessage(
							m.to,
							DEFAULT_SMSC,
							parts,
							intentsFor(SENDING_REPORT, m, parts),
							intentsFor(DELIVERY_REPORT, m, parts));
				}
			} else {
				logEvent(ctx, "Not sending SMS to '%s' because number appears invalid (content: '%s')",
						m.to, m.content);
				db.setFailed(m, "destination.invalid");
			}
		}
	}

	private void sendSms_dummy(WoMessage m) {
		logEvent(ctx, "sendSms_dummy() :: [%s] '%s'", m.to, m.content);
		db.updateStatus(m, UNSENT, PENDING);
		db.updateStatus(m, PENDING, SENT);
		db.updateStatus(m, SENT, DELIVERED);
	}

	private ArrayList<PendingIntent> intentsFor(String intentType, WoMessage m, ArrayList<String> parts) {
		int totalParts = parts.size();
		ArrayList<PendingIntent> intents = new ArrayList<>(totalParts);
		for(int partIndex=0; partIndex<totalParts; ++partIndex) {
			intents.add(intentFor(intentType, m, partIndex, totalParts));
		}
		return intents;
	}

	@SuppressLint("UnspecifiedImmutableFlag")
	private PendingIntent intentFor(String intentType, WoMessage m, int partIndex, int totalParts) {
		Intent intent = new Intent(ctx, IntentProcessor.class);
		intent.setAction(intentType);
		intent.putExtra("id", m.id);
		intent.putExtra("partIndex", partIndex);
		intent.putExtra("totalParts", totalParts);

		return PendingIntent.getBroadcast(ctx, m.id.hashCode(), intent, PendingIntent.FLAG_ONE_SHOT);
	}

	/**
	 * I haven't read the specs closely, but it's a good starting point to
	 * assume that CDMA SMS can be sent in ASCII mode (8 bits per character)
	 * or UTF-16 mode (16 bits per character).
	 */
	static final ArrayList<String> divideMessageForCdma(String content) {
		ArrayList<String> parts = new ArrayList<>();

		int perMessageCharLimit = onlyExtendedAscii(content) ? 140 : 70;

		if(content.length() <= perMessageCharLimit) {
			parts.add(content);
		} else {
			// Leave space for the `n/N ` part indicator.
			perMessageCharLimit -= 4;

			// This code could save 9 characters for messages with 10 parts or more, and
			// more for messages with 100 or more parts, but it doesn't seem worth the
			// effort handling these cases.  Also, if there are more than 999 parts we'll
			// be in trouble.
			int partCount = content.length() / perMessageCharLimit;
			if(partCount >= 10) partCount = content.length() / --perMessageCharLimit;
			if(partCount >= 100) partCount = content.length() / --perMessageCharLimit;

			for(int i=0; i<partCount; ++i) {
				int startIndex = i*perMessageCharLimit;
				int endIndex = Math.min(content.length(), startIndex + perMessageCharLimit);
				parts.add(String.format("%s/%s %s", i+1, partCount,
						content.substring(startIndex, endIndex)));
			}
		}

		return parts;
	}

	/**
	 * Assumes CDMA 8-bit is ISO-8859-1.  Java uses UTF-16 for char values,
	 * which is identical to ISO-8859-1 in the first 256 characters.
	 **/
	private static boolean onlyExtendedAscii(String s) {
		for(int i=s.length()-1; i>=0; --i)
			if(s.charAt(i) > 255) return false;
		return true;
	}

	private List<WoMessage> getUnsentMessages() {
		List<WoMessage> unsentSms = db.getWoMessages(MAX_WO_MESSAGES, UNSENT);
		List<WoMessage> smsForSending = new ArrayList<>();

		for(WoMessage sms : unsentSms) {
			if (sms.retries > 0) {
				if (sms.canRetryAfterSoftFail()) {
					smsForSending.add(sms);
				}
			} else {
				smsForSending.add(sms);
			}
		}

		return smsForSending;
	}
}
