package medic.gateway;

import android.app.*;
import android.content.*;
import android.os.*;

import java.io.*;

import org.json.*;

import static android.app.Service.*;
import static medic.gateway.BuildConfig.DEBUG;
import static medic.gateway.DebugLog.logEvent;

public class WebappPoller extends Service {
	public int onStartCommand(Intent intent, int flags, int startId) {
		logEvent(this, "WebappPoller.onStartCommand()");

		System.err.println("#################################");
		System.err.println("# WebappPoller.onStartCommand() #");
		System.err.println("#################################");

		new Thread() {
			public void run() {
				try {
					pollWebapp();
				} catch(Exception ex) {
					if(DEBUG) ex.printStackTrace();
				} finally {
					WebappPoller.this.stopSelf();
				}
			}
		}.start();

		return START_STICKY;
	}

	private void pollWebapp() throws IOException, JSONException {
		SettingsStore settings = SettingsStore.in(this);
		JSONObject res = new SimpleJsonClient2().get(settings.getWebappUrl() + "/api/v1/messages"); // TODO check proper URL
		if(DEBUG) log(res.toString());
		JSONArray messages = res.getJSONArray("messages");
		for(int i=0; i<messages.length(); ++i) {
			try {
				saveMessage(messages.getJSONObject(i));
			} catch(Exception ex) {
				if(DEBUG) ex.printStackTrace();
			}
		}
	}

	private void saveMessage(JSONObject m) throws JSONException {
		WoRepo.$.save(new WoMessage(m.getString("to"), m.getString("message")));
	}

	public IBinder onBind(Intent _) { return null; }

	private void log(String message, Object...extras) {
		if(DEBUG) System.err.println("LOG | WebappPoller :: " +
				String.format(message, extras));
	}
}

