package medic.gateway.alert.test;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.test.rule.ActivityTestRule;

import medic.gateway.alert.*;

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
}
