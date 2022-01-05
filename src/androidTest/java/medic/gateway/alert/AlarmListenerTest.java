package medic.gateway.alert;

import android.annotation.SuppressLint;
import android.app.*;
import android.content.*;
import android.test.*;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import org.junit.*;

import static android.app.PendingIntent.FLAG_ONE_SHOT;
import static org.junit.Assert.*;

@SuppressLint("CommitPrefEdits")
@SuppressWarnings({"PMD.SignatureDeclareThrowsException"})
public class AlarmListenerTest extends AndroidTestCase {

	@Test
	public void test_scheduleAlarms_shouldDoNothing_whenThereAreNoSettings() throws Exception {
		// given
		noSettings();
		AlarmListener alarmListener = new AlarmListener();

		// when
		alarmListener.scheduleAlarms(alarmManager(), aPendingIntent(), getContext());

		// then
		// No exception was thrown
	}

	private PendingIntent aPendingIntent() {
		Intent i = new Intent(getContext(), WakefulIntentService.class);
		return PendingIntent.getService(getContext(), 0, i, FLAG_ONE_SHOT);
	}

	private AlarmManager alarmManager() {
		return (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
	}

	private void noSettings() {
		SharedPreferences prefs = getContext().getSharedPreferences(
				SettingsStore.class.getName(),
				Context.MODE_PRIVATE);
		prefs.edit().clear().commit();

		assertNull(Settings.in(getContext()));
	}
}
