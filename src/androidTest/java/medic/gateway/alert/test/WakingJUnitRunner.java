package medic.gateway.alert.test;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.PowerManager;
import androidx.test.runner.AndroidJUnitRunner;

import static android.content.Context.KEYGUARD_SERVICE;
import static android.content.Context.POWER_SERVICE;
import static android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP;
import static android.os.PowerManager.FULL_WAKE_LOCK;
import static android.os.PowerManager.ON_AFTER_RELEASE;

/**
 * The purpose of this test runner is to make sure that the android device/
 * emulator is awake and does not have screenlock enabled when the tests start.
 * Otherwise, we may see the error: "Waited for the root of the view hierarchy
 * to have window focus and not be requesting layout for over 10 seconds."
 */
public class WakingJUnitRunner extends AndroidJUnitRunner {
	private PowerManager.WakeLock lock;

	@SuppressWarnings("deprecation") @SuppressLint("MissingPermission")
	public void onStart() {
		Context ctx = getTargetContext().getApplicationContext();

		KeyguardManager k = (KeyguardManager) ctx.getSystemService(KEYGUARD_SERVICE);
		k.newKeyguardLock(KEYGUARD_SERVICE).disableKeyguard();

		PowerManager power = (PowerManager) ctx.getSystemService(POWER_SERVICE);
		lock = power.newWakeLock(ACQUIRE_CAUSES_WAKEUP | FULL_WAKE_LOCK | ON_AFTER_RELEASE, getClass().getSimpleName());
		lock.acquire();

		super.onStart();
	}

	public void onDestroy() {
		super.onDestroy();

		lock.release();
	}
}
