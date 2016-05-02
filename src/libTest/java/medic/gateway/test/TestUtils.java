package medic.gateway.test;

import android.annotation.*;
import android.app.*;
import android.content.*;
import android.util.*;
import android.view.*;

import medic.gateway.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

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

	public static String decodeBase64(String encodedString) {
		try {
			return new String(Base64.decode(encodedString, Base64.DEFAULT), "UTF-8");
		} catch(UnsupportedEncodingException ex) {
			throw new RuntimeException(ex);
		}
	}
}
