package medic.gateway.test;

import java.util.regex.*;

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
}
