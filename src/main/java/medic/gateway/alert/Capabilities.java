package medic.gateway.alert;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.provider.Telephony;

import static medic.gateway.alert.BuildConfig.APPLICATION_ID;
import static medic.gateway.alert.GatewayLog.logEvent;

@SuppressWarnings("PMD.ClassWithOnlyPrivateConstructorsShouldBeFinal") // Class isn't final, so it can be mocked in tests.
public class Capabilities {

	private Capabilities() {}

	@TargetApi(19)
	public boolean isDefaultSmsProvider(Context ctx) {
		if (!canBeDefaultSmsProvider()) {
			throw new IllegalStateException("Gateway can not be the default SMS Provider. SDK: " + Build.VERSION.SDK_INT);
		}

		String defaultSMSProvider = Telephony.Sms.getDefaultSmsPackage(ctx);
		logEvent(ctx, "Default SMS Provider: %s", defaultSMSProvider);

		return APPLICATION_ID.equals(defaultSMSProvider);
	}

	/**
	 * Check if cht-gateway can be the default messaging app on this device.
	 * This feature is only available on Android 4.4 (kitkat®) or later.
	 */
	@SuppressLint("ObsoleteSdkInt") // lint seems to think checking for > KITKAT is unnecessary: "Error: Unnecessary; SDK_INT is never < 16", but I think KITKAT is version 19 or 20
	public boolean canBeDefaultSmsProvider() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
	}

	public static Capabilities getCapabilities() {
		return new Capabilities();
	}
}
