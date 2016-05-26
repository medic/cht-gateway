package medic.gateway;

import android.app.*;
import android.content.*;
import android.os.*;

import java.io.*;
import java.net.*;
import java.util.*;

import org.json.*;

import static medic.gateway.BuildConfig.DEBUG;
import static medic.gateway.GatewayLog.*;
import static medic.gateway.Utils.json;

public class WebappPoller {
	private static final int MAX_WT_MESSAGES = 10;
	private static final int MAX_WO_MESSAGES = 10;

	private final Context ctx;
	private final Db db;

	public WebappPoller(Context ctx) {
		this.ctx = ctx;
		this.db = Db.getInstance(ctx);
	}

	public void pollWebapp() throws JSONException, MalformedURLException {
		GatewayRequest request = new GatewayRequest(
				db.getWtMessages(MAX_WT_MESSAGES, WtMessage.Status.WAITING),
				db.getWoMessagesWithStatusChanges(MAX_WO_MESSAGES));

		logEvent(ctx, "Polling webapp (forwarding %d messages & %d status updates)...",
				request.wtMessageCount(), request.statusUpdateCount());

		SettingsStore settings = SettingsStore.in(ctx);
		SimpleResponse response = new SimpleJsonClient2().post(settings.getWebappUrl(), request.getJson());
		if(DEBUG) log(response.toString());

		if(response.isError()) {
			handleError(response);
		} else {
			handleJsonResponse(request, ((JsonResponse) response).json);
		}
	}

	private void handleJsonResponse(GatewayRequest request, JSONObject response) throws JSONException {
		for(WtMessage m : request.messages) {
			try {
				db.updateStatusFrom(WtMessage.Status.WAITING, m);
			} catch(Exception ex) {
				logException(ctx, ex, "WebappPoller::Error updating WT message %s status: %s", m.id, ex.getMessage());
			}
		}

		for(WoMessage m : request.statusUpdates) {
			try {
				db.setStatusForwarded(m);
			} catch(Exception ex) {
				logException(ctx, ex, "WebappPoller::Error updating WO message %s status_forwarded value: %s", m.id, ex.getMessage());
			}
		}

		if(response.isNull("messages")) {
			return;
		}

		JSONArray messages = response.getJSONArray("messages");

		logEvent(ctx, "Received %d SMS from server for sending.", messages.length());

		for(int i=0; i<messages.length(); ++i) {
			try {
				saveMessage(messages.getJSONObject(i));
			} catch(Exception ex) {
				logException(ex, "WebappPoller.handleJsonResponse()");
			}
		}
	}

	private void handleError(SimpleResponse response) throws JSONException {
		String description = "unknown";

		if(response instanceof JsonResponse) {
			JsonResponse jsonResponse = (JsonResponse) response;
			if(jsonResponse.json.has("message")) description = jsonResponse.json.getString("message");
		}

		logEvent(ctx, "Received error from server: %s: %s", response.status, description);
	}

	private void saveMessage(JSONObject json) throws JSONException {
		logEvent(ctx, "Saving WO message: %s", json);
		WoMessage m = new WoMessage(
				json.getString("id"),
				json.getString("to"),
				json.getString("content"));
		boolean success = db.store(m);
		if(!success) logEvent(ctx, "Failed to save WO message: %s", json);
	}

	private void log(String message, Object...extras) {
		trace("WebappPoller", message, extras);
	}
}

class GatewayRequest {
	final List<WtMessage> messages;
	final List<WoMessage> statusUpdates;

	GatewayRequest(List<WtMessage> messages, List<WoMessage> statusUpdates) {
		this.messages = messages;
		this.statusUpdates = statusUpdates;
	}

	int wtMessageCount() { return messages.size(); }
	int statusUpdateCount() { return statusUpdates.size(); }

	public JSONObject getJson() throws JSONException {
		JSONObject json = new JSONObject();
		json.put("messages", getMessagesJson());
		json.put("updates", getStatusUpdateJson());
		return json;
	}

	private JSONArray getMessagesJson() {
		JSONArray json = new JSONArray();

		for(WtMessage m : messages) {
			try {
				json.put(json(
					"id", m.id,
					"from", m.from,
					"content", m.content
				));
				m.setStatus(WtMessage.Status.FORWARDED);
			} catch(Exception ex) {
				logException(ex, "GatewayRequest.getMessagesJson()");
				m.setStatus(WtMessage.Status.FAILED);
			}
		}

		return json;
	}

	private JSONArray getStatusUpdateJson() {
		JSONArray json = new JSONArray();

		for(WoMessage m : statusUpdates) {
			try {
				JSONObject deliveryUpdate = json(
					"id", m.id,
					"status", m.status.toString()
				);
				if(m.status == WoMessage.Status.FAILED) {
					deliveryUpdate.put("reason", m.getFailureReason());
				}
				json.put(deliveryUpdate);
			} catch(Exception ex) {
				logException(ex, "GatewayRequest.getStatusUpdateJson()");
			}
		}

		return json;
	}
}
