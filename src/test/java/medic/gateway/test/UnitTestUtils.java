package medic.gateway.test;

import java.lang.reflect.*;

import medic.gateway.*;

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
}
