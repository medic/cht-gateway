package medic.gateway.alert;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.text.Html;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;
import java.text.SimpleDateFormat;

import org.json.JSONException;
import org.json.JSONObject;

import static java.util.UUID.randomUUID;

import static medic.gateway.alert.BuildConfig.DEBUG;
import static medic.gateway.alert.Capabilities.getCapabilities;
import static medic.gateway.alert.GatewayLog.logException;
import static medic.gateway.alert.GatewayLog.trace;

@SuppressWarnings({"PMD.ModifiedCyclomaticComplexity",
		"PMD.NPathComplexity",
		"PMD.StdCyclomaticComplexity"})
public final class Utils {
	private static final long ONE_MINUTE = 1000 * 60;
	private static final long ONE_HOUR = ONE_MINUTE * 60;
	private static final long ONE_DAY = ONE_HOUR * 24;

	private static final long TWO_DAYS = ONE_DAY * 2;
	private static final long ONE_WEEK = ONE_DAY * 7;
	private static final long ONE_MONTH = ONE_WEEK * 4;
	private static final long ONE_YEAR = ONE_MONTH * 12;

	public static final DialogInterface.OnClickListener NO_CLICK_LISTENER = null;

	private Utils() {}

	public static String getAppName(Context ctx) {
		return ctx.getResources().getString(R.string.app_name);
	}

	public static String getAppVersion(Context ctx) {
		try {
			return ctx.getPackageManager()
					.getPackageInfo(ctx.getPackageName(), 0)
					.versionName;
		} catch(Exception ex) {
			return "?";
		}
	}

	public static String normalisePhoneNumber(String phoneNumber) {
		return phoneNumber.replaceAll("[-\\s]", "");
	}

	public static String randomUuid() {
		return randomUUID().toString();
	}

	public static void toast(Context ctx, int messageId, Object... args) {
		toast(ctx, ctx.getString(messageId), args);
	}

	public static void toast(Context ctx, String message, Object... args) {
		Toast.makeText(ctx, String.format(message, args), Toast.LENGTH_LONG).show();
	}

	public static void showAlert(final Activity parent, final AlertDialog.Builder dialog) {
		parent.runOnUiThread(new Runnable() {
			public void run() {
				if(parent.isFinishing()) return;
				dialog.create().show();
			}
		});
	}

	public static JSONObject json(Object... keyVals) throws JSONException {
		if(DEBUG && keyVals.length % 2 != 0) throw new AssertionError();
		JSONObject o = new JSONObject();
		for(int i=keyVals.length-1; i>0; i-=2) {
			o.put(keyVals[i-1].toString(), keyVals[i]);
		}
		return o;
	}

	public static String absoluteTimestamp(long timestamp) {
		return SimpleDateFormat.getDateTimeInstance()
				.format(new Date(timestamp));
	}

	public static String relativeTimestamp(long timestamp) {
		long diff = System.currentTimeMillis() - timestamp;

		if(diff < ONE_MINUTE) {
			return "just now";
		}

		if(diff < ONE_HOUR) {
			long mins = diff / ONE_MINUTE;
			return mins + "m ago";
		}

		if(diff < ONE_DAY) {
			long hours = diff / ONE_HOUR;
			return hours + "h ago";
		}

		if(diff < TWO_DAYS) return "yesterday";

		if(diff < ONE_WEEK) {
			long days = diff / ONE_DAY;
			return days + " days ago";
		}

		if(diff < ONE_MONTH) {
			long weeks = diff / ONE_WEEK;
			if(weeks == 1) return "a week ago";
			return weeks + " weeks ago";
		}

		if(diff < ONE_YEAR) {
			long months = diff / ONE_MONTH;
			if(months == 1) return "a month ago";
			return months + " months ago";
		}

		long years = diff / ONE_YEAR;
		if(years == 1) return "a year ago";
		return years + " years ago";
	}

	public static String[] args(String... args) {
		return args;
	}

	public static String[] args(Object... args) {
		String[] strings = new String[args.length];
		for(int i=args.length-1; i>=0; --i) {
			strings[i] = args[i] == null? null: args[i].toString();
		}
		return strings;
	}

	public static void startSettingsActivity(Context ctx, Capabilities app) {
		Class activity;
		if(app.canBeDefaultSmsProvider() && !app.isDefaultSmsProvider(ctx)) {
			activity = PromptToSetAsDefaultMessageAppActivity.class;
		} else {
			activity = SettingsDialogActivity.class;
		}
		ctx.startActivity(new Intent(ctx, activity));
	}

	public static void setText(View v, int textViewId, String text) {
		TextView tv = (TextView) v.findViewById(textViewId);
		tv.setText(text);
	}

	public static void setText(Activity a, int textViewId, int stringId, Object... args) {
		TextView text = (TextView) a.findViewById(textViewId);
		text.setText(Html.fromHtml(a.getResources().getString(stringId, args)));
	}

	public static void startMainActivity(final Context ctx) {
		AsyncTask.execute(new Runnable() {
			public void run() {
				AlarmListener.restart(ctx);
			}
		});
		ctx.startActivity(new Intent(ctx, MessageListsActivity.class));
	}

	public static void startSettingsOrMainActivity(Context ctx) {
		if(SettingsStore.in(ctx).hasSettings()) {
			trace(ctx, "Starting MessageListsActivity...");
			startMainActivity(ctx);
		} else {
			trace(ctx, "Starting settings activity...");
			startSettingsActivity(ctx, getCapabilities());
		}
	}

	public static void includeVersionNameInActivityTitle(Activity a) {
		try {
			String versionName = a.getPackageManager().getPackageInfo(a.getPackageName(), 0).versionName;
			a.setTitle(a.getTitle() + " " + versionName);
		} catch(Exception ex) {
			logException(ex, "Could not include the version number in the page title.");
		}
	}
}
