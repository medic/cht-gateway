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

public class WakefulService extends WakefulIntentService {
	private final Context ctx;
	public WakefulService() {
		super("WakefulService");
		this.ctx = this;
	}

	public WakefulService(Context ctx) {
		super("WakefulService");
		this.ctx = ctx;
	}

	protected Db getDbInstance(){
		return Db.getInstance(MedicGetwayApplication.getMedicApplicationContext());
	}

	protected Context getCtxInstance(){
		return this.ctx;
	}

	protected WebappPoller getWebappPoller(){
		return new WebappPoller(getCtxInstance());
	}


	@SuppressWarnings({ "PMD.AvoidDeeplyNestedIfStmts", "PMD.StdCyclomaticComplexity", "PMD.ModifiedCyclomaticComplexity", "PMD.NPathComplexity" })
	public void doWakefulWork(Intent intent) {
		boolean enableWifiAfterWork = false;
		WifiConnectionManager wifiMan = null;

		try {
			getDbInstance().cleanLogs();
		} catch(Exception ex) {
			logException(this, ex, "Exception caught trying to clean up event log: %s", ex.getMessage());
		}

		try {
			SimpleResponse lastResponse = getWebappPoller().pollWebapp();
			getWebappPoller().pollWebappMessagesAvailable();
			boolean messagesAvailable;

			do {
				try {
					// TODO check if we should be handling other failures in addition to timeouts e.g. java.net.SocketException
					if (lastResponse instanceof ExceptionResponse) {
						ExceptionResponse exResponse = (ExceptionResponse) lastResponse;
						if (exResponse.ex instanceof SocketTimeoutException ||
								exResponse.ex instanceof UnknownHostException ||
								exResponse.ex instanceof ConnectException ||
								exResponse.ex instanceof NoRouteToHostException) {
							wifiMan = new WifiConnectionManager(this);
							if (wifiMan.isWifiActive()) {
								logEvent(this, "Disabling wifi and then retrying poll...");
								enableWifiAfterWork = true;
								wifiMan.disableWifi();
								lastResponse = getWebappPoller().pollWebapp();
							}
						}
					}

					if (lastResponse == null || lastResponse.isError()) {
						LastPoll.failed(MedicGetwayApplication.getMedicApplicationContext());
					} else {
						LastPoll.succeeded(MedicGetwayApplication.getMedicApplicationContext());
					}

				} catch (Exception ex) {
					logException(this, ex, "Exception caught trying to poll webapp: %s", ex.getMessage());
					LastPoll.failed(this);
				} finally {
					LastPoll.broadcast(this);
				}

				try {
					new SmsSender(MedicGetwayApplication.getMedicApplicationContext()).sendUnsentSmses();
				} catch (Exception ex) {
					logException(this, ex, "Exception caught trying to send SMSes: %s", ex.getMessage());
				}

				if (enableWifiAfterWork) try {
					wifiMan.enableWifi();
				} catch (Exception ex) {
					logException(this, ex, "Exception caught trying to check wifi status: %s", ex.getMessage());
				}
				messagesAvailable = getWebappPoller().pollWebappMessagesAvailable();
			}
			while (messagesAvailable);
		}
		catch (Exception ex){
			logException(this,ex,"Exception caught while looping messages batch: %s", ex.getMessage());
		}
	}
}
