package medic.gateway.test;

import java.util.regex.*;

import static org.junit.Assert.*;

public final class TestUtils {
	private TestUtils() {}

	public static void assertMatches(Object pattern, Object actual) {
		assertTrue(((Pattern) pattern).matcher(actual.toString()).matches());
	}

	public static void assertMatches(String failureMessage, Object pattern, Object actual) {
		assertTrue(failureMessage, ((Pattern) pattern).matcher(actual.toString()).matches());
	}
}
