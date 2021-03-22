package medic.gateway.alert;

import android.content.Context;
import android.content.Intent;

import com.commonsware.cwac.wakeful.WakefulIntentService;

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
		try {
			Db.getInstance(ctx).cleanLogs();
		} catch (Exception ex) {
			logException(ctx, ex, "Exception caught trying to clean up event log: %s", ex.getMessage());
		}

		try {
			WebappPoller poller;
			boolean keepPollingWebapp = true;

			while (keepPollingWebapp) {
				poller = new WebappPoller(ctx);
				SimpleResponse lastResponse = poller.pollWebapp();

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
		} catch (Exception ex) {
			logException(ctx, ex, "Exception caught trying to send SMSes: %s", ex.getMessage());
		}
	}
}
