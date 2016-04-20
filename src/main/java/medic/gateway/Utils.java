package medic.gateway;

import android.content.*;
import android.provider.*;

import java.util.*;

import org.json.*;

public class Utils {
	private static final String[] RANDOM_WORDS = {
		"I", "a", "action", "admirable", "air", "all", "an", "and", "angel", "animals", "appears", "apprehension", "beauty", "brave", "but", "canopy", "congregation", "custom", "delights", "disposition", "dust", "earth", "excellent", "exercises", "express", "faculty", "fire", "firmament", "forgone", "form", "foul", "frame", "fretted", "god", "goes", "golden", "goodly", "have", "heavily", "how", "in", "indeed", "infinite", "is", "it", "know", "late", "like", "look", "lost", "majestical", "man", "me", "mirth", "most", "moving", "my", "neither", "no", "noble", "nor", "not", "of", "other", "overhanging", "paragon", "pestilential", "piece", "promontory", "quintessence", "reason", "roof", "seems", "so", "sterile", "than", "that", "the", "thing", "this", "to", "vapours", "what", "wherefore", "why", "with", "woman", "work", "world", "yet", "you",
	};

	private static final long ONE_MINUTE = 1000 * 60;
	private static final long ONE_HOUR = ONE_MINUTE * 60;
	private static final long ONE_DAY = ONE_HOUR * 24;
	private static final long TWO_DAYS = ONE_DAY * 2;
	private static final long ONE_WEEK = ONE_DAY * 7;
	private static final long ONE_MONTH = ONE_WEEK * 4;
	private static final long ONE_YEAR = ONE_MONTH * 12;

	public static JSONObject json(Object... keyVals) throws JSONException {
		assert keyVals.length % 2 == 0;
		JSONObject o = new JSONObject();
		for(int i=keyVals.length-1; i>0; i-=2) {
			o.put(keyVals[i-1].toString(), keyVals[i].toString());
		}
		return o;
	}

	public static String randomPhoneNumber() {
		Random r = new Random();
		StringBuilder bob = new StringBuilder();
		bob.append('+');
		for(int i=0; i<10; ++i) bob.append(r.nextInt(10));
		return bob.toString();
	}

	public static String randomSmsContent() {
		Random r = new Random();
		int wordCount = r.nextInt(20) + 1;
		StringBuilder bob = new StringBuilder();
		for(int i=0; i<wordCount; ++i) {
			bob.append(' ');
			bob.append(RANDOM_WORDS[r.nextInt(RANDOM_WORDS.length)]);
		}
		return bob.substring(1);
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
			return weeks + " weeks ago";
		}

		if(diff < ONE_YEAR) {
			long months = diff / ONE_MONTH;
			return months + " months ago";
		}

		long years = diff / ONE_YEAR;
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

	public static boolean isDefaultSmsProvider(Context ctx) {
		return Utils.class.getPackage().getName().equals(Telephony.Sms.getDefaultSmsPackage(ctx));
	}
}
