package medic.gateway.alert.test;

import android.app.*;

import java.lang.reflect.*;

import medic.gateway.alert.*;

import org.robolectric.shadows.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public final class UnitTestUtils {
	private UnitTestUtils() {}

	public static Capabilities mockCapabilities(Object objectWithCapabilities) {
		try {
			Capabilities mockCapabilities = mock(Capabilities.class);

			Field f = objectWithCapabilities.getClass().getDeclaredField("app");
			f.setAccessible(true);
			f.set(objectWithCapabilities, mockCapabilities);

			return mockCapabilities;
		} catch(Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public static void assertActivityLaunched(ShadowApplication shadowApplication, Class<? extends Activity> expected) {
		String expectedName = String.format("%s/%s",
				expected.getPackage().getName(),
				expected.getName());
		String actualName = shadowApplication.getNextStartedActivity().getComponent().flattenToString();
		assertEquals(expectedName, actualName);
	}
}
