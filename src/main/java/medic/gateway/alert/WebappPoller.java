package medic.gateway.alert;

import android.app.*;
import android.content.*;
import android.os.*;

import java.io.*;
import java.net.*;
import java.util.*;

import org.json.*;

import static medic.gateway.alert.BuildConfig.DEBUG;
import static medic.gateway.alert.GatewayLog.*;
import static medic.gateway.alert.Utils.json;

public class WebappPoller {
	private static final int MAX_WT_MESSAGES = 10;
	private static final int MAX_WOM_STATUS_UPDATES = 20;

	private final Context ctx;
	private final Db db;

	private final GatewayRequest request;
	private final String webappUrl;

	public WebappPoller(Context ctx) {
		this.ctx = ctx;
		db = Db.getInstance(ctx);

		request = new GatewayRequest(
				db.getWtMessages(MAX_WT_MESSAGES, WtMessage.Status.WAITING),
				db.getWoMessageStatusUpdates(MAX_WOM_STATUS_UPDATES));

		webappUrl = Settings.in(ctx).webappUrl;
	}

	public SimpleResponse pollWebapp() throws JSONException, MalformedURLException {
		logEvent(ctx, "Polling webapp (forwarding %d messages & %d status updates)...",
				request.wtMessageCount(), request.statusUpdateCount());

		SimpleResponse response = new SimpleJsonClient2().post(webappUrl, request.getJson());
		if(DEBUG) log(response.toString());

		if(response.isError()) {
			handleError(response);
		} else {
			handleJsonResponse(request, ((JsonResponse) response).json);
		}

		return response;
	}

	private void handleJsonResponse(GatewayRequest request, JSONObject response) throws JSONException {
		for(WtMessage m : request.messages) {
			try {
				db.updateStatusFrom(WtMessage.Status.WAITING, m);
			} catch(Exception ex) {
				logException(ctx, ex, "WebappPoller::Error updating WT message %s status: %s", m.id, ex.getMessage());
			}
		}

		for(WoMessage.StatusUpdate u : request.statusUpdates) {
			try {
				db.setStatusForwarded(u);
			} catch(Exception ex) {
				logException(ctx, ex, "WebappPoller::Error updating WO message status %s as forwarded: %s", u, ex.getMessage());
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
		} else {
			ExceptionResponse errorResponse = (ExceptionResponse) response;
			description = errorResponse.ex.toString();
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
	final List<WoMessage.StatusUpdate> statusUpdates;

	GatewayRequest(List<WtMessage> messages, List<WoMessage.StatusUpdate> statusUpdates) {
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
					"content", m.content,
					"sms_sent", m.smsSent,
					"sms_received", m.smsReceived
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

		for(WoMessage.StatusUpdate u : statusUpdates) {
			try {
				JSONObject deliveryUpdate = json(
					"id", u.messageId,
					"status", u.newStatus.toString()
				);
				if(u.newStatus == WoMessage.Status.FAILED) {
					deliveryUpdate.put("reason", u.failureReason);
				}
				json.put(deliveryUpdate);
			} catch(Exception ex) {
				logException(ex, "GatewayRequest.getStatusUpdateJson()");
			}
		}

		return json;
	}
}
