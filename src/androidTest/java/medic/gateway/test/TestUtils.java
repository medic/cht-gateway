package medic.gateway.test;

import android.annotation.*;
import android.app.*;
import android.content.*;
import android.support.test.rule.*;
import android.util.*;
import android.view.*;

import medic.gateway.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import static android.content.Context.KEYGUARD_SERVICE;
import static android.support.test.InstrumentationRegistry.*;
import static org.junit.Assert.*;

public final class TestUtils {
	public static final String A_PHONE_NUMBER = "+447890123123";
	public static final String SOME_CONTENT = "Hello.";
	public static final Pattern ANY_NUMBER = Pattern.compile("\\d+");
	public static final Pattern ANY_ID = Pattern.compile("[a-f0-9-]+");

	private TestUtils() {}

	public static void assertMatches(Object pattern, Object actual) {
		assertMatches(null, pattern, actual);
	}

	public static void assertMatches(String failureMessage, Object pattern, Object actual) {
		boolean matches = ((Pattern) pattern).matcher(actual.toString()).matches();
		if(!matches) {
			if(failureMessage == null) {
				failureMessage = "";
			} else {
				failureMessage += ": ";
			}
			fail(String.format("%s\"%s\" did not match regex /%s/", failureMessage, actual, pattern));
		}
	}

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

	public static String decodeBase64(String encodedString) {
		try {
			return new String(Base64.decode(encodedString, Base64.DEFAULT), "UTF-8");
		} catch(UnsupportedEncodingException ex) {
			throw new RuntimeException(ex);
		}
	}
}
