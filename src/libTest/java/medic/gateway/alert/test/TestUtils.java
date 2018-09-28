package medic.gateway.alert.test;

import android.annotation.*;
import android.app.*;
import android.content.*;
import android.util.Base64;
import android.view.*;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import medic.gateway.alert.*;

import java.io.*;
import java.util.Random;
import java.util.List;
import java.util.regex.*;

import static org.junit.Assert.*;

/**
 * There is a chance that the PDUs listed in this class are invalid on CDMA
 * devices.  This has the potential to break tests run on those devices.
 */
@SuppressFBWarnings("MS_MUTABLE_ARRAY")
public final class TestUtils {
	public static final String A_PHONE_NUMBER = "+447890123123";
	public static final String ANOTHER_PHONE_NUMBER = "+447890000000";
	public static final Pattern ANY_PHONE_NUMBER = Pattern.compile("\\+\\d{7,12}");
	public static final String SOME_CONTENT = "Hello.";
	public static final Pattern ANY_CONTENT = Pattern.compile(".*");
	public static final Pattern ANY_NUMBER = Pattern.compile("\\d+");
	public static final Pattern GT_ZERO = Pattern.compile("[1-9]+\\d*");
	public static final Pattern ANY_ID = Pattern.compile("[a-f0-9-]+");

	public static final byte[] A_VALID_GSM_PDU = {
		(byte) 0x07, (byte) 0x91, (byte) 0x44, (byte) 0x77, (byte) 0x28, (byte) 0x00, (byte) 0x80, (byte) 0x00, (byte) 0x04, (byte) 0x0c, (byte) 0x91, (byte) 0x44, (byte) 0x87, (byte) 0x09, (byte) 0x21, (byte) 0x43, (byte) 0x65, (byte) 0x00, (byte) 0x00, (byte) 0x90, (byte) 0x20, (byte) 0x11, (byte) 0x31, (byte) 0x74, (byte) 0x63, (byte) 0x00, (byte) 0x23, (byte) 0xc7, (byte) 0xf7, (byte) 0x9b, (byte) 0x0c, (byte) 0x32, (byte) 0xbf, (byte) 0xe5, (byte) 0xa0, (byte) 0xfc, (byte) 0xbb, (byte) 0xee, (byte) 0x02, (byte) 0x4d, (byte) 0xd9, (byte) 0x61, (byte) 0x38, (byte) 0xe8, (byte) 0xed, (byte) 0x06, (byte) 0xd1, (byte) 0xd1, (byte) 0x65, (byte) 0x90, (byte) 0x38, (byte) 0x3c, (byte) 0x5e, (byte) 0x83, (byte) 0xca, (byte) 0xf4, (byte) 0xb1, (byte) 0x0b,
	};

	public static final byte[] A_VALID_GSM_PDU_FROM_THE_MULTIPART_SENDER = {
		(byte) 0x07, (byte) 0x91, (byte) 0x44, (byte) 0x77, (byte) 0x28, (byte) 0x00, (byte) 0x80, (byte) 0x00, (byte) 0x04, (byte) 0x0c, (byte) 0x91, (byte) 0x44, (byte) 0x87, (byte) 0x09, (byte) 0x99, (byte) 0x99, (byte) 0x99, (byte) 0x00, (byte) 0x00, (byte) 0x90, (byte) 0x20, (byte) 0x11, (byte) 0x31, (byte) 0x74, (byte) 0x63, (byte) 0x00, (byte) 0x23, (byte) 0xc7, (byte) 0xf7, (byte) 0x9b, (byte) 0x0c, (byte) 0x32, (byte) 0xbf, (byte) 0xe5, (byte) 0xa0, (byte) 0xfc, (byte) 0xbb, (byte) 0xee, (byte) 0x02, (byte) 0x4d, (byte) 0xd9, (byte) 0x61, (byte) 0x38, (byte) 0xe8, (byte) 0xed, (byte) 0x06, (byte) 0xd1, (byte) 0xd1, (byte) 0x65, (byte) 0x90, (byte) 0x38, (byte) 0x3c, (byte) 0x5e, (byte) 0x83, (byte) 0xca, (byte) 0xf4, (byte) 0xb1, (byte) 0x0b,
	};

	public static final byte[] A_VALID_MULTIPART_GSM_PDU__PART_1 = {
		(byte) 0x07, (byte) 0x91, (byte) 0x44, (byte) 0x97, (byte) 0x37, (byte) 0x01, (byte) 0x90, (byte) 0x37, (byte) 0x64, (byte) 0x0C, (byte) 0x91, (byte) 0x44, (byte) 0x87, (byte) 0x09, (byte) 0x99, (byte) 0x99, (byte) 0x99, (byte) 0x00, (byte) 0x00, (byte) 0x90, (byte) 0x20, (byte) 0x11, (byte) 0x71, (byte) 0x85, (byte) 0x31, (byte) 0x00, (byte) 0xA0, (byte) 0x06, (byte) 0x08, (byte) 0x04, (byte) 0xB9, (byte) 0xDB, (byte) 0x02, (byte) 0x01, (byte) 0xC8, (byte) 0xB2, (byte) 0xBC, (byte) 0x0C, (byte) 0x4A, (byte) 0xCF, (byte) 0x41, (byte) 0x61, (byte) 0x90, (byte) 0xBD, (byte) 0x2C, (byte) 0xCF, (byte) 0x83, (byte) 0xEC, (byte) 0x65, (byte) 0x79, (byte) 0x1E, (byte) 0x64, (byte) 0x2F, (byte) 0xCB, (byte) 0xF3, (byte) 0x20, (byte) 0x7B, (byte) 0x59, (byte) 0x9E, (byte) 0x07, (byte) 0xD9, (byte) 0xCB, (byte) 0xF2, (byte) 0x3C, (byte) 0xC8, (byte) 0x5E, (byte) 0x96, (byte) 0xE7, (byte) 0x41, (byte) 0xF6, (byte) 0xB2, (byte) 0x3C, (byte) 0x0F, (byte) 0xB2, (byte) 0x97, (byte) 0xE5, (byte) 0x79, (byte) 0x90, (byte) 0xBD, (byte) 0x2C, (byte) 0xCF, (byte) 0x83, (byte) 0xEC, (byte) 0x65, (byte) 0x79, (byte) 0x1E, (byte) 0x64, (byte) 0x2F, (byte) 0xCB, (byte) 0xF3, (byte) 0x20, (byte) 0x7B, (byte) 0x59, (byte) 0x9E, (byte) 0x07, (byte) 0xD9, (byte) 0xCB, (byte) 0xF2, (byte) 0x3C, (byte) 0xC8, (byte) 0x5E, (byte) 0x96, (byte) 0xE7, (byte) 0x41, (byte) 0xF6, (byte) 0xB2, (byte) 0x3C, (byte) 0x0F, (byte) 0xB2, (byte) 0x97, (byte) 0xE5, (byte) 0x79, (byte) 0x90, (byte) 0xBD, (byte) 0x2C, (byte) 0xCF, (byte) 0x83, (byte) 0xEC, (byte) 0x65, (byte) 0x79, (byte) 0x1E, (byte) 0xC4, (byte) 0x7E, (byte) 0xBB, (byte) 0xCF, (byte) 0xA0, (byte) 0x76, (byte) 0x79, (byte) 0x3E, (byte) 0x0F, (byte) 0x9F, (byte) 0xCB, (byte) 0xA0, (byte) 0x3B, (byte) 0x3A, (byte) 0x3D, (byte) 0x46, (byte) 0x83, (byte) 0xC2, (byte) 0x63, (byte) 0x7A, (byte) 0x3D, (byte) 0xCC, (byte) 0x66, (byte) 0xE7, (byte) 0x41, (byte) 0x73, (byte) 0x78, (byte) 0xD8, (byte) 0x3D, (byte) 0x07, (byte) 0xD1, (byte) 0xEF, (byte) 0x6F, (byte) 0xD0, (byte) 0x9B, (byte) 0x8E, (byte) 0x2E, (byte) 0xCB, (byte) 0x41, (byte) 0xED, (byte) 0xF2, (byte) 0x7C, (byte) 0x1E, (byte) 0x3E, (byte) 0x97, (byte) 0xE7,
	};
	public static final byte[] A_VALID_MULTIPART_GSM_PDU__PART_2 = {
		(byte) 0x07, (byte) 0x91, (byte) 0x44, (byte) 0x97, (byte) 0x37, (byte) 0x01, (byte) 0x90, (byte) 0x37, (byte) 0x64, (byte) 0x0C, (byte) 0x91, (byte) 0x44, (byte) 0x87, (byte) 0x09, (byte) 0x99, (byte) 0x99, (byte) 0x99, (byte) 0x00, (byte) 0x00, (byte) 0x90, (byte) 0x20, (byte) 0x11, (byte) 0x71, (byte) 0x85, (byte) 0x63, (byte) 0x00, (byte) 0x25, (byte) 0x06, (byte) 0x08, (byte) 0x04, (byte) 0xB9, (byte) 0xDB, (byte) 0x02, (byte) 0x02, (byte) 0xA0, (byte) 0x30, (byte) 0x3C, (byte) 0x2C, (byte) 0xA7, (byte) 0x83, (byte) 0xCC, (byte) 0xF2, (byte) 0x77, (byte) 0x1B, (byte) 0x44, (byte) 0x47, (byte) 0x97, (byte) 0x41, (byte) 0x6F, (byte) 0x79, (byte) 0xFA, (byte) 0x9C, (byte) 0x76, (byte) 0x87, (byte) 0xD9, (byte) 0xA0, (byte) 0xB7, (byte) 0xBB, (byte) 0x1C, (byte) 0x02,
	};

	public static final byte[] A_VALID_DELIVERED_REPORT = {
		(byte) 0x07, (byte) 0x91, (byte) 0x52, (byte) 0x74, (byte) 0x22, (byte) 0x05, (byte) 0x00, (byte) 0x00, (byte) 0x06, (byte) 0x07, (byte) 0x0A, (byte) 0x81, (byte) 0x70, (byte) 0x20, (byte) 0x95, (byte) 0x77, (byte) 0x11, (byte) 0x11, (byte) 0x21, (byte) 0x12, (byte) 0x11, (byte) 0x33, (byte) 0x91, (byte) 0xE1, (byte) 0x11, (byte) 0x21, (byte) 0x12, (byte) 0x11, (byte) 0x33, (byte) 0x91, (byte) 0x21, (byte) 0x00,
	};

	private static final Random RANDOM = new Random();

	private TestUtils() {}

	public static void assertMatches(Object pattern, Object actual) {
		assertMatches(null, pattern, actual);
	}

	public static void assertMatches(String failureMessage, Object pattern, Object actual) {
		if(pattern == null) throw new IllegalArgumentException();
		if(actual == null) fail(String.format("%s\"null\" did not match regex /%s/", failureMessage, pattern));
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

	public static <T> void assertListEquals(List<T> actual, T...expected) {
		assertArrayEquals(expected, actual.toArray());
	}

	public static String decodeBase64(String encodedString) {
		try {
			return new String(Base64.decode(encodedString, Base64.DEFAULT), "UTF-8");
		} catch(UnsupportedEncodingException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static long now() {
		return System.currentTimeMillis();
	}

	public static long daysAgo(long numberOfDaysAgo) {
		return now() - (numberOfDaysAgo * 1000 * 60 * 60 * 24);
	}

	public static int randomInt(int limit) {
		return RANDOM.nextInt(limit);
	}
}
