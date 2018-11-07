package medic.gateway.alert;

import android.os.Build;
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

	private SmsCompatibility() {}

	@SuppressLint("NewApi") // Available in older APIs, but not officially public
	public static SmsMessage[] getMessagesFromIntent(Intent i) {
		return android.provider.Telephony.Sms.Intents.getMessagesFromIntent(i);
	}

	/**
	 * @see https://developer.android.com/reference/android/telephony/SmsMessage.html#createFromPdu%28byte[],%20java.lang.String%29
	 */
	@SuppressLint("ObsoleteSdkInt") // lint seems to think checking for > M is unnecessary: "Error: Unnecessary; SDK_INT is never < 16", but I think M is version 23
	public static SmsMessage createFromPdu(Intent intent) {
		byte[] pdu = intent.getByteArrayExtra("pdu");
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			String format = intent.getStringExtra("format");
			return SmsMessage.createFromPdu(pdu, format);
		} else {
			return SmsMessage.createFromPdu(pdu);
		}
	}
}
