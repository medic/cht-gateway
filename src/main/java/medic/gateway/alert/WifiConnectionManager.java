package medic.gateway.alert;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.Context.WIFI_SERVICE;
import static android.net.ConnectivityManager.TYPE_WIFI;

import static medic.gateway.alert.GatewayLog.logEvent;

/**
 * This class aims to detect when the current wifi connection is not functioning
 * correctly, and disable or re-enable the wifi accordingly.
 */
class WifiConnectionManager {
	private final Context ctx;

	public WifiConnectionManager(Context ctx) {
		this.ctx = ctx;
	}

	public boolean isWifiActive() {
		logEvent(ctx, "Checking if wifi is active...");
		ConnectivityManager cMan = (ConnectivityManager) ctx.getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo info = cMan.getActiveNetworkInfo();
		boolean wifiActive = info != null && info.getType() == TYPE_WIFI;

		logEvent(ctx, "Wifi active? %s [%s]", wifiActive, info);

		return wifiActive;
	}

	public void enableWifi() {
		logEvent(ctx, "Enabling wifi...");
		getWifiManager().setWifiEnabled(true);
	}

	public void disableWifi() {
		logEvent(ctx, "Disabling wifi...");
		getWifiManager().setWifiEnabled(false);
	}

	private WifiManager getWifiManager() {
		return (WifiManager) ctx.getSystemService(WIFI_SERVICE);
	}
}
