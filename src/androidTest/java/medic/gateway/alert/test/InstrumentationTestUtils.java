package medic.gateway.alert.test;

import android.annotation.*;
import android.app.*;
import android.content.*;
import android.support.test.rule.*;
import android.util.*;
import android.view.*;

import medic.gateway.alert.*;

import java.io.*;
import java.util.*;

import static android.content.Context.KEYGUARD_SERVICE;
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

	@SuppressWarnings("deprecation") @SuppressLint("MissingPermission")
	public static void preventScreenLock(final ActivityTestRule testRule) throws Throwable {
		testRule.runOnUiThread(new Runnable() {
			public void run() {
				Activity activity = testRule.getActivity();

				// prevent lock
				KeyguardManager k = (KeyguardManager) activity.getSystemService(KEYGUARD_SERVICE);
				k.newKeyguardLock(KEYGUARD_SERVICE).disableKeyguard();

				// turn on screen and remove any current locks
				activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
						WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
						WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
						WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			};
		});
	}
}
