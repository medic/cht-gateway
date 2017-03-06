package medic.gateway.alert;

import android.content.Intent;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import java.net.SocketTimeoutException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;

import static medic.gateway.alert.GatewayLog.*;

public class WakefulService extends WakefulIntentService {
	public WakefulService() {
		super("WakefulService");
	}

	@SuppressWarnings("PMD.AvoidDeeplyNestedIfStmts")
	public void doWakefulWork(Intent intent) {
		boolean enableWifiAfterWork = false;
		WifiConnectionManager wifiMan = null;

		try {
			Db.getInstance(this).cleanLogs();
		} catch(Exception ex) {
			logException(this, ex, "Exception caught trying to clean up event log: %s", ex.getMessage());
		}

		try {
			WebappPoller poller = new WebappPoller(this);
			SimpleResponse resp = poller.pollWebapp();

			// TODO check if we should be handling other failures in addition to timeouts e.g. java.net.SocketException
			if(resp instanceof ExceptionResponse) {
				ExceptionResponse exResponse = (ExceptionResponse) resp;
				if(exResponse.ex instanceof SocketTimeoutException ||
						exResponse.ex instanceof UnknownHostException ||
						exResponse.ex instanceof ConnectException ||
						exResponse.ex instanceof NoRouteToHostException) {
					wifiMan = new WifiConnectionManager(this);
					if(wifiMan.isWifiActive()) {
						logEvent(this, "Disabling wifi and then retrying poll...");
						enableWifiAfterWork = true;
						wifiMan.disableWifi();
						poller.pollWebapp();
					}
				}
			}
		} catch(Exception ex) {
			logException(this, ex, "Exception caught trying to poll webapp: %s", ex.getMessage());
		}

		try {
			new SmsSender(this).sendUnsentSmses();
		} catch(Exception ex) {
			logException(this, ex, "Exception caught trying to send SMSes: %s", ex.getMessage());
		}

		if(enableWifiAfterWork) try {
			wifiMan.enableWifi();
		} catch(Exception ex) {
			logException(this, ex, "Exception caught trying to check wifi status: %s", ex.getMessage());
		}
	}
}
