package medic.gateway;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.telephony.SmsMessage;

/**
 * Because the android SMS API was not official before android-19, redefining
 * these constants and methods here allows us to avoid various lint warnings/
 * suppressions elsewhere in the code.
 */
final class SmsCompatibility {
	@SuppressLint("NewApi") // Available in older APIs, but not officially public
	public static final String SMS_RECEIVED_ACTION =
			android.provider.Telephony.Sms.Intents.SMS_RECEIVED_ACTION;

	/**
	 * Should only be used in android-19 and above; {@code TargetApi}
	 * annotation cannot be applied to a constant, otherwise it would be
	 * used here.
	 */
	@SuppressLint("NewApi")
	public static final String SMS_DELIVER_ACTION =
			android.provider.Telephony.Sms.Intents.SMS_DELIVER_ACTION;

	@SuppressLint("NewApi") // Available in older APIs, but not officially public
	public static final String WAP_PUSH_DELIVER_ACTION =
			android.provider.Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION;

	@SuppressLint("NewApi") // Available in older APIs, but not officially public
	public static final Uri SMS_INBOX = android.provider.Telephony.Sms.Inbox.CONTENT_URI;

	@SuppressLint("NewApi") // Available in older APIs, but not officially public
	public static SmsMessage[] getMessagesFromIntent(Intent i) {
		return android.provider.Telephony.Sms.Intents.getMessagesFromIntent(i);
	}

	private SmsCompatibility() {}
}
