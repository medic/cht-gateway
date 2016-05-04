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
	/** There's a chance this PDU could be invalid on CDMA phones, which would break tests :¬( */
	public static final byte[] A_VALID_GSM_PDU = {
		(byte) 0x07, (byte) 0x91, (byte) 0x44, (byte) 0x77, (byte) 0x28, (byte) 0x00, (byte) 0x80, (byte) 0x00, (byte) 0x04, (byte) 0x0c, (byte) 0x91, (byte) 0x44, (byte) 0x87, (byte) 0x09, (byte) 0x21, (byte) 0x43, (byte) 0x65, (byte) 0x00, (byte) 0x00, (byte) 0x90, (byte) 0x20, (byte) 0x11, (byte) 0x31, (byte) 0x74, (byte) 0x63, (byte) 0x00, (byte) 0x23, (byte) 0xc7, (byte) 0xf7, (byte) 0x9b, (byte) 0x0c, (byte) 0x32, (byte) 0xbf, (byte) 0xe5, (byte) 0xa0, (byte) 0xfc, (byte) 0xbb, (byte) 0xee, (byte) 0x02, (byte) 0x4d, (byte) 0xd9, (byte) 0x61, (byte) 0x38, (byte) 0xe8, (byte) 0xed, (byte) 0x06, (byte) 0xd1, (byte) 0xd1, (byte) 0x65, (byte) 0x90, (byte) 0x38, (byte) 0x3c, (byte) 0x5e, (byte) 0x83, (byte) 0xca, (byte) 0xf4, (byte) 0xb1, (byte) 0x0b,
	};

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
