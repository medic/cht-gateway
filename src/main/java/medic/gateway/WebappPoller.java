package medic.gateway;

import android.app.*;
import android.content.*;
import android.os.*;

import java.io.*;
import java.util.*;

import org.json.*;

import static medic.gateway.BuildConfig.DEBUG;
import static medic.gateway.DebugLog.logEvent;
import static medic.gateway.Utils.json;

public class WebappPoller {
	private static final int MAX_WT_MESSAGES = 10;

	private final Context ctx;
	private final Db db;

	public WebappPoller(Context ctx) {
		this.ctx = ctx;
		this.db = Db.getInstance(ctx);
	}

	public void pollWebapp() throws IOException, JSONException {
		JSONObject request = buildRequest();

		SettingsStore settings = SettingsStore.in(ctx);
		SimpleResponse response = new SimpleJsonClient2().post(settings.getWebappUrl(), request);
		if(DEBUG) log(response.toString());

		if(response.isError()) {
			handleError(response);
		} else {
			handleJsonResponse(((JsonResponse) response).json);
		}
	}

	private void handleJsonResponse(JSONObject response) throws JSONException {
		if(!response.has("messages")) {
			return;
		}

		JSONArray messages = response.getJSONArray("messages");
		for(int i=0; i<messages.length(); ++i) {
			try {
				saveMessage(messages.getJSONObject(i));
			} catch(Exception ex) {
				if(DEBUG) ex.printStackTrace();
			}
		}
	}

	private JSONObject buildRequest() throws JSONException {
		JSONObject request = new JSONObject();
		request.put("messages", getWebappTerminatingMessages());
		request.put("deliveries", getDeliveryReports());
		return request;
	}

	private JSONArray getWebappTerminatingMessages() {
		JSONArray messages = new JSONArray();

		List<WtMessage> waitingMessages = db.getWtMessages(MAX_WT_MESSAGES, WtMessage.Status.WAITING);
		for(WtMessage m : waitingMessages) {
			try {
				messages.put(json(
					"id", m.id,
					"from", m.from,
					"content", m.content
				));
				m.setStatus(WtMessage.Status.FORWARDED);
			} catch(Exception ex) {
				if(DEBUG) ex.printStackTrace();
				logEvent(ctx, "Failed to create json for message: " + m);
				m.setStatus(WtMessage.Status.FAILED);
			}
		}
		db.update(waitingMessages);

		return messages;
	}

	private JSONArray getDeliveryReports() {
		// TODO we'll handle this when we actually support delivery reports
		return new JSONArray();
	}

	private void handleError(SimpleResponse response) throws JSONException {
		String description = "unknown";

		if(response instanceof JsonResponse) {
			JsonResponse jsonResponse = (JsonResponse) response;
			if(jsonResponse.json.has("message")) description = jsonResponse.json.getString("message");
		}

		logEvent(ctx, "Received error from server: " + response.status + ": " + description);
	}

	private void saveMessage(JSONObject json) throws JSONException {
		logEvent(ctx, "Saving WO message: " + json);
		WoMessage m = new WoMessage(
				json.getString("id"),
				json.getString("to"),
				json.getString("content"));
		boolean success = db.store(m);
		if(!success) logEvent(ctx, "Failed to save WO message: " + json);
	}

	private void log(String message, Object...extras) {
		if(DEBUG) System.err.println("LOG | WebappPoller :: " +
				String.format(message, extras));
	}
}
