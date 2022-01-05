package medic.gateway.alert.test;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.TextView;
import androidx.lifecycle.Lifecycle;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewAssertion;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import java.util.Iterator;
import medic.gateway.alert.*;
import org.json.*;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isNotChecked;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.platform.app.InstrumentationRegistry.*;
import static medic.gateway.alert.R.*;
import static medic.gateway.alert.test.BuildConfig.IS_MEDIC_FLAVOUR;
import static org.junit.Assert.*;

public final class InstrumentationTestUtils {
	private InstrumentationTestUtils() {}

	public static void clearAppSettings() {
		SharedPreferences prefs = getInstrumentation().getTargetContext().getSharedPreferences(
				SettingsStore.class.getName(),
				Context.MODE_PRIVATE);
		SharedPreferences.Editor ed = prefs.edit();
		ed.clear();
		assertTrue(ed.commit());
	}

	public static void recreateActivityFor(final ActivityScenarioRule testRule) {
		testRule.getScenario().moveToState(Lifecycle.State.RESUMED).recreate();
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

	public static void assertNotChecked(int cbxId) {
		onView(withId(cbxId)).check(matches(isNotChecked()));
	}

	public static void assertErrorDisplayed(int errorMessageResourceId) {
		int componentId = IS_MEDIC_FLAVOUR ? id.txtWebappInstanceName : id.txtWebappUrl;
		assertErrorDisplayed(componentId, errorMessageResourceId);
	}

	public static void assertErrorDisplayed(int componentId, int errorMessageResourceId) {
		String errorString = getApplicationContext().getResources().getString(errorMessageResourceId);
		assertErrorDisplayed(componentId, errorString);
	}

	public static void assertErrorDisplayed(int componentId, final String expectedMessage) {
		onView(withId(componentId))
				.check(new ViewAssertion() {
					public void check(View view, NoMatchingViewException noViewFoundException) {
						if(!(view instanceof TextView))
							fail("Supplied view is not a TextView, so does not have an error property.");
						TextView tv = (TextView) view;
						assertEquals(expectedMessage, tv.getError());
					}
				});
	}

	public static void assertVisible(int viewId) {
		onView(withId(viewId)).perform(scrollTo()).check(matches(isDisplayed()));
	}

	@SuppressWarnings("PMD.EmptyCatchBlock")
	public static void assertDoesNotExist(int viewId) {
		try {
			onView(withId(viewId)).check(matches(isDisplayed()));
			fail("Found view which should not exist!");
		} catch(NoMatchingViewException ex) {
			// expected
		}
	}

	public static Settings settings() {
		return Settings.in(getApplicationContext());
	}

	public static SettingsStore settingsStore() {
		return SettingsStore.in(getApplicationContext());
	}

	public static void enterText(int componentId, String text) {
		onView(withId(componentId))
				.perform(typeText(text), closeSoftKeyboard());
	}
}
