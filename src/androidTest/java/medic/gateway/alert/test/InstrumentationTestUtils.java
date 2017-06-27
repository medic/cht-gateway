package medic.gateway.alert.test;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.test.rule.ActivityTestRule;

import java.util.Iterator;

import medic.gateway.alert.*;

import org.json.*;

import static android.support.test.InstrumentationRegistry.*;
import static org.junit.Assert.*;

public final class InstrumentationTestUtils {
	private InstrumentationTestUtils() {}

	public static void clearAppSettings() {
		SharedPreferences prefs = getTargetContext().getSharedPreferences(
				SettingsStore.class.getName(),
				Context.MODE_PRIVATE);
		SharedPreferences.Editor ed = prefs.edit();
		ed.clear();
		assertTrue(ed.commit());
	}

	public static void recreateActivityFor(final ActivityTestRule testRule) {
		getInstrumentation().runOnMainSync(new Runnable() {
			public void run() {
				testRule.getActivity().recreate();
			}
		});
	}

	public static void assertJson(String expected, String actual) throws JSONException {
		assertJson(new JSONObject(expected), actual);
	}

	public static void assertJson(JSONObject expected, String actual) throws JSONException {
		assertJson(expected, new JSONObject(actual));
	}

	public static void assertJson(JSONObject expected, JSONObject actual) throws JSONException {
		if(!areEqual(expected, actual)) assertEquals(expected.toString(), actual.toString());
	}

	public static void assertJson(String message, String expected, String actual) throws JSONException {
		assertJson(message, new JSONObject(expected), actual);
	}

	public static void assertJson(String message, JSONObject expected, String actual) throws JSONException {
		assertJson(message, expected, new JSONObject(actual));
	}

	public static void assertJson(String message, JSONObject expected, JSONObject actual) throws JSONException {
		if(!areEqual(expected, actual)) assertEquals(message, expected.toString(), actual.toString());
	}

	private static boolean areEqual(JSONObject a, JSONObject b) throws JSONException {
		if(a.equals(b) || a.toString().equals(b.toString())) return true;

		Iterator<String> keys = a.keys();
		while(keys.hasNext()) {
			String k = keys.next();

			if(!b.has(k)) return false;

			if(!areJsonValuesEqual(a.get(k), b.get(k))) return false;
		}

		return true;
	}

	@SuppressWarnings({"PMD.NPathComplexity", "PMD.ModifiedCyclomaticComplexity", "PMD.StdCyclomaticComplexity"})
	private static boolean areJsonValuesEqual(Object a, Object b) throws JSONException {
		if(a instanceof Boolean    && b instanceof Boolean)    return ((boolean) a) == ((boolean) b);
		if(a instanceof Double     && b instanceof Double)     return ((double)  a) == ((double)  b);
		if(a instanceof Integer    && b instanceof Integer)    return ((int)     a) == ((int)     b);
		if(a instanceof Long       && b instanceof Long)       return ((long)    a) == ((long)    b);
		if(a instanceof String     && b instanceof String)     return ((String) a).equals(b);
		if(a instanceof JSONObject && b instanceof JSONObject) return areEqual((JSONObject) a, (JSONObject) b);
		if(a instanceof JSONArray  && b instanceof JSONArray) {
			JSONArray aa = (JSONArray) a;
			JSONArray bb = (JSONArray) b;

			if(aa.length() != bb.length()) return false;
			for(int i=aa.length()-1; i>=0; --i)
				if(!areJsonValuesEqual(aa.get(i), bb.get(i)))
					return false;
			return true;
		}
		return false;
	}
}
