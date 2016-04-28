package medic.gateway.test;

import java.util.regex.*;

import static org.junit.Assert.*;

public final class TestUtils {
	public static final String[] NO_ARGS = {};
	public static final String ALL_ROWS = null;

	private TestUtils() {}

	public static String[] args(String... args) { return args; }

	public static void assertMatches(Object pattern, Object actual) {
		assertTrue(((Pattern) pattern).matcher(actual.toString()).matches());
	}

	public static void assertMatches(String failureMessage, Object pattern, Object actual) {
		assertTrue(failureMessage, ((Pattern) pattern).matcher(actual.toString()).matches());
	}
}
