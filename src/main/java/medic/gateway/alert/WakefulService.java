package medic.gateway.alert;

import android.content.Context;
import android.content.Intent;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import java.net.SocketTimeoutException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;

import static medic.gateway.alert.GatewayLog.logEvent;
import static medic.gateway.alert.GatewayLog.logException;

// TODO: WakefulIntentService is dead and should be replaced with official code in
//       Android Jetpack. See: https://github.com/commonsguy/cwac-wakeful
public class WakefulService extends WakefulIntentService {
	private final Context ctx;

	public WakefulService() {
		super("WakefulService");

		this.ctx = this;
	}

	WakefulService(Context ctx) {
		super("WakefulService");

		this.ctx = ctx;
	}

	@SuppressWarnings({ "PMD.AvoidDeeplyNestedIfStmts", "PMD.StdCyclomaticComplexity", "PMD.ModifiedCyclomaticComplexity", "PMD.NPathComplexity" })
	public void doWakefulWork(Intent intent) {

		Settings settings = Settings.in(ctx);
		WifiConnectionManager wifiMan = null;
		boolean wifiAutoEnableSetting = settings == null ? false : settings.wifiAutoEnable;
		boolean enableWifiAfterWork = false;
		boolean keepPollingWebapp = true;

		try {
			Db.getInstance(ctx).cleanLogs();
		} catch(Exception ex) {
			logException(ctx, ex, "Exception caught trying to clean up event log: %s", ex.getMessage());
		}

		try {
			WebappPoller poller;

			while (keepPollingWebapp) {
				poller = new WebappPoller(ctx);
				SimpleResponse lastResponse = poller.pollWebapp();

				if (lastResponse instanceof ExceptionResponse) {
					ExceptionResponse exResponse = (ExceptionResponse) lastResponse;

					if (exResponse.ex instanceof SocketTimeoutException
							|| exResponse.ex instanceof UnknownHostException
							|| exResponse.ex instanceof ConnectException
							|| exResponse.ex instanceof NoRouteToHostException) {

						wifiMan = new WifiConnectionManager(ctx);

						if (wifiAutoEnableSetting && wifiMan.isWifiActive()) {
							logEvent(ctx, "Disabling wifi and then retrying poll...");
							enableWifiAfterWork = true;
							wifiMan.disableWifi();
							lastResponse = poller.pollWebapp();
						}
					}
				}

				if (lastResponse == null || lastResponse.isError()) {
					LastPoll.failed(ctx);
				} else {
					LastPoll.succeeded(ctx);
				}

				/**
				 * NB: this is only testing that *we* have more to send to webapp, and not that webapp has
				 * more to send to us. To enable that feature correctly we should have webapp pass us this
				 * value back, because otherwise we'd have to hardcode webapp's batch size in Gateway
				 */
				keepPollingWebapp = poller.moreMessagesToSend(lastResponse);
			}

		} catch (Exception ex) {
			logException(ctx, ex, "Exception caught trying to poll webapp: %s", ex.getMessage());
			LastPoll.failed(ctx);
		} finally {
			LastPoll.broadcast(ctx);
		}

		try {
			new SmsSender(ctx).sendUnsentSmses();
		} catch(Exception ex) {
			logException(ctx, ex, "Exception caught trying to send SMSes: %s", ex.getMessage());
		}

		if (wifiAutoEnableSetting && enableWifiAfterWork) {
			try {
				wifiMan.enableWifi();
			} catch(Exception ex) {
				logException(ctx, ex, "Exception caught trying to check wifi status: %s", ex.getMessage());
			}
		}
	}
}
