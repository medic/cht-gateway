package medic.gateway.alert;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.net.MalformedURLException;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static medic.gateway.alert.BuildConfig.DEBUG;
import static medic.gateway.alert.GatewayLog.logEvent;
import static medic.gateway.alert.GatewayLog.logException;
import static medic.gateway.alert.GatewayLog.trace;
import static medic.gateway.alert.SimpleJsonClient2.uriEncodeAuth;
import static medic.gateway.alert.Utils.normalisePhoneNumber;
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
				db.getWoMessageStatusUpdates(MAX_WOM_STATUS_UPDATES)
		);

		webappUrl = Settings.in(ctx).webappUrl;
	}

//> PUBLIC API
	public SimpleResponse pollWebapp() throws JSONException, MalformedURLException {
		logEvent(ctx, "Polling webapp (forwarding %d messages & %d status updates)...",
				request.wtMessageCount(), request.statusUpdateCount());

		SimpleResponse response = new SimpleJsonClient2()
				.post(uriEncodeAuth(webappUrl), request.getJson());

		if (DEBUG) {
			log(response.toString());
		}

		if (response.isError()) {
			handleError(response);
		} else {
			handleOkResponse(request, ((JsonResponse) response).json);
		}

		return response;
	}

	public Boolean moreMessagesToSend(SimpleResponse lastResponse) {
		if (lastResponse == null || lastResponse.isError()) {
			return false;
		}

		return request.wtMessageCount() == MAX_WT_MESSAGES || request.statusUpdateCount() == MAX_WOM_STATUS_UPDATES;
	}

//> PRIVATE HELPERS
	private void handleOkResponse(GatewayRequest request, JSONObject response) throws JSONException {
		for (WtMessage m : request.messages) {
			try {
				db.updateStatusFrom(WtMessage.Status.WAITING, m);
			} catch (Exception ex) {
				logException(ctx, ex, "WebappPoller::Error updating WT message %s status: %s", m.id, ex.getMessage());
			}
		}

		for (WoMessage.StatusUpdate u : request.statusUpdates) {
			try {
				db.setStatusForwarded(u);
			} catch (Exception ex) {
				logException(ctx, ex, "WebappPoller::Error updating WO message status %s as forwarded: %s", u, ex.getMessage());
			}
		}

		if (response.isNull("messages")) {
			return;
		}

		JSONArray messages = response.getJSONArray("messages");

		logEvent(ctx, "Received %d SMS from server for sending.", messages.length());

		for (int i=0; i < messages.length(); ++i) {
			try {
				saveMessage(messages.getJSONObject(i));
			} catch (Exception ex) {
				logException(ex, "WebappPoller.handleOkResponse()");
			}
		}
	}

	private void handleError(SimpleResponse response) throws JSONException {
		CharSequence description = "unknown";

		if (response instanceof JsonResponse) {
			JsonResponse jsonResponse = (JsonResponse) response;

			if (jsonResponse.json.has("message")) {
				description = jsonResponse.json.getString("message");
			}

		} else if (response instanceof ExceptionResponse) {
			description = ((ExceptionResponse) response).ex.toString();

		} else if (response instanceof TextResponse) {
			description = ((TextResponse) response).text;
		}

		logEvent(ctx, "Received error from server: %s: %s", response.status, description);
	}

	private void saveMessage(JSONObject json) throws JSONException {
		logEvent(ctx, "Saving WO message: %s", json);
		WoMessage m = new WoMessage(
				json.getString("id"),
				normalisePhoneNumber(json.getString("to")),
				json.getString("content")
		);

		boolean success = db.store(m);

		if (!success) {
			logEvent(ctx, "Failed to save WO message: %s", json);
		}
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

		for (WtMessage m : messages) {
			try {
				json.put(json(
					"id", m.id,
					"from", m.from,
					"content", m.content,
					"sms_sent", m.smsSent,
					"sms_received", m.smsReceived
				));
				m.setStatus(WtMessage.Status.FORWARDED);
			} catch (Exception ex) {
				logException(ex, "GatewayRequest.getMessagesJson()");
				m.setStatus(WtMessage.Status.FAILED);
			}
		}

		return json;
	}

	private JSONArray getStatusUpdateJson() {
		JSONArray json = new JSONArray();

		for (WoMessage.StatusUpdate u : statusUpdates) {
			try {
				JSONObject deliveryUpdate = json(
					"id", u.messageId,
					"status", u.newStatus.toString()
				);
				if (u.newStatus == WoMessage.Status.FAILED) {
					deliveryUpdate.put("reason", u.failureReason);
				}
				json.put(deliveryUpdate);
			} catch (Exception ex) {
				logException(ex, "GatewayRequest.getStatusUpdateJson()");
			}
		}

		return json;
	}
}

class LastPoll {
	private static final String INTENT_UPDATED = "medic.gateway.WebappPoller.UPDATED";

	private static final String PREF_LAST_TIMESTAMP = "last-timestamp";
	private static final String PREF_LAST_WAS_SUCCESSFUL = "last-was-successful";

//> INSTANCE
	final long timestamp;
	final boolean wasSuccessful;
	LastPoll(long timestamp, boolean wasSuccessful) {
		this.timestamp = timestamp;
		this.wasSuccessful = wasSuccessful;
	}

//> PUBLIC UTILITIES
	static void succeeded(Context ctx) {
		logLast(ctx, true);
	}

	static void failed(Context ctx) {
		logLast(ctx, false);
	}

	static LastPoll getFrom(Context ctx) {
		SharedPreferences prefs = prefs(ctx);

		if (!prefs.contains(PREF_LAST_TIMESTAMP) || !prefs.contains(PREF_LAST_WAS_SUCCESSFUL)) {
			return null;
		} else {
			return new LastPoll(prefs.getLong(PREF_LAST_TIMESTAMP, 0),
					prefs.getBoolean(PREF_LAST_WAS_SUCCESSFUL, true));
		}
	}

	static boolean isStatusUpdate(Intent i) {
		return INTENT_UPDATED.equals(i.getAction());
	}

	public static void register(Context ctx, BroadcastReceiver receiver) {
		IntentFilter f = new IntentFilter();
		f.addAction(INTENT_UPDATED);
		LocalBroadcastManager.getInstance(ctx).registerReceiver(receiver, f);
	}

	public static void unregister(Context ctx, BroadcastReceiver receiver) {
		LocalBroadcastManager.getInstance(ctx).unregisterReceiver(receiver);
	}

	public static void broadcast(Context ctx) {
		Intent i = new Intent(INTENT_UPDATED);
		LocalBroadcastManager.getInstance(ctx).sendBroadcast(i);
	}

//> STATIC HELPERS
	private static SharedPreferences prefs(Context ctx) {
		return ctx.getSharedPreferences(WebappPoller.class.getName(), Context.MODE_PRIVATE);
	}

	private static void logLast(Context ctx, boolean success) {
		SharedPreferences.Editor e = prefs(ctx).edit();
		e.putLong(PREF_LAST_TIMESTAMP, System.currentTimeMillis());
		e.putBoolean(PREF_LAST_WAS_SUCCESSFUL, success);
		e.apply();
	}
}
