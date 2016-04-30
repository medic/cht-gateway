package medic.gateway.test;

import android.app.*;
import android.content.*;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;

import medic.gateway.*;

import java.util.*;
import java.util.regex.*;

import static android.support.test.runner.lifecycle.Stage.RESUMED;
import static android.support.test.InstrumentationRegistry.*;
import static org.junit.Assert.*;

public final class TestUtils {
	public static final String A_PHONE_NUMBER = "+447890123123";
	public static final String SOME_CONTENT = "Hello.";

	private TestUtils() {}

	public static void assertMatches(Object pattern, Object actual) {
		assertTrue(((Pattern) pattern).matcher(actual.toString()).matches());
	}

	public static void assertMatches(String failureMessage, Object pattern, Object actual) {
		assertTrue(failureMessage, ((Pattern) pattern).matcher(actual.toString()).matches());
	}

	public static void clearAppSettings() {
		SharedPreferences prefs = getTargetContext().getSharedPreferences(
				SettingsStore.class.getName(),
				Context.MODE_PRIVATE);
		SharedPreferences.Editor ed = prefs.edit();
		ed.clear();
		assertTrue(ed.commit());
	}

	public static void recreateCurrentActivity() {
		getInstrumentation().runOnMainSync(new Runnable() {
			public void run() {
				Collection resumedActivities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(RESUMED);
				assertEquals(1, resumedActivities.size());
				((Activity) resumedActivities.toArray()[0]).recreate();
			}
		});
	}
}
