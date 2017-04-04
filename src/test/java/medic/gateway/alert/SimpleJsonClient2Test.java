package medic.gateway.alert;

import android.app.*;
import android.content.*;

import org.junit.*;
import org.junit.runner.*;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.*;

import static medic.gateway.alert.test.UnitTestUtils.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.robolectric.Shadows.*;

@Config(constants=BuildConfig.class)
public class SimpleJsonClient2Test {
	private static final String NO_CHANGE = null;

	@Test
	public void redactUrl_shouldRemovePasswordsWhenProvided() {
		final String[] testData = {
			"random-string", NO_CHANGE,
			"random string with spaces", NO_CHANGE,
			"http://normal.url", NO_CHANGE,
			"http://normal.url/with/path", NO_CHANGE,
			"http://ported.url:80", NO_CHANGE,
			"http://ported.url:80/with/path", NO_CHANGE,
			"http://:@normal.url", "http://:****@normal.url",
			"http://:@normal.url/with/path", "http://:****@normal.url/with/path",
			"http://:@ported.url:80", "http://:****@ported.url:80",
			"http://:@ported.url:80/with/path", "http://:****@ported.url:80/with/path",
			"http://user:@normal.url", "http://user:****@normal.url",
			"http://user:@normal.url/with/path", "http://user:****@normal.url/with/path",
			"http://user:@ported.url:80", "http://user:****@ported.url:80",
			"http://user:@ported.url:80/with/path", "http://user:****@ported.url:80/with/path",
			"http://:pass@normal.url", "http://:****@normal.url",
			"http://:pass@normal.url/with/path", "http://:****@normal.url/with/path",
			"http://:pass@ported.url:80", "http://:****@ported.url:80",
			"http://:pass@ported.url:80/with/path", "http://:****@ported.url:80/with/path",
			"http://user:pass@normal.url", "http://user:****@normal.url",
			"http://user:pass@normal.url/with/path", "http://user:****@normal.url/with/path",
			"http://user:pass@ported.url:80", "http://user:****@ported.url:80",
			"http://user:pass@ported.url:80/with/path", "http://user:****@ported.url:80/with/path",
			"https://normal.url", NO_CHANGE,
			"https://normal.url/with/path", NO_CHANGE,
			"https://ported.url:80", NO_CHANGE,
			"https://ported.url:80/with/path", NO_CHANGE,
			"https://:@normal.url", "https://:****@normal.url",
			"https://:@normal.url/with/path", "https://:****@normal.url/with/path",
			"https://:@ported.url:80", "https://:****@ported.url:80",
			"https://:@ported.url:80/with/path", "https://:****@ported.url:80/with/path",
			"https://user:@normal.url", "https://user:****@normal.url",
			"https://user:@normal.url/with/path", "https://user:****@normal.url/with/path",
			"https://user:@ported.url:80", "https://user:****@ported.url:80",
			"https://user:@ported.url:80/with/path", "https://user:****@ported.url:80/with/path",
			"https://:pass@normal.url", "https://:****@normal.url",
			"https://:pass@normal.url/with/path", "https://:****@normal.url/with/path",
			"https://:pass@ported.url:80", "https://:****@ported.url:80",
			"https://:pass@ported.url:80/with/path", "https://:****@ported.url:80/with/path",
			"https://user:pass@normal.url", "https://user:****@normal.url",
			"https://user:pass@normal.url/with/path", "https://user:****@normal.url/with/path",
			"https://user:pass@ported.url:80", "https://user:****@ported.url:80",
			"https://user:pass@ported.url:80/with/path", "https://user:****@ported.url:80/with/path",
			"ftp://normal.url", NO_CHANGE,
			"ftp://normal.url/with/path", NO_CHANGE,
			"ftp://ported.url:80", NO_CHANGE,
			"ftp://ported.url:80/with/path", NO_CHANGE,
			"ftp://:@normal.url", "ftp://:****@normal.url",
			"ftp://:@normal.url/with/path", "ftp://:****@normal.url/with/path",
			"ftp://:@ported.url:80", "ftp://:****@ported.url:80",
			"ftp://:@ported.url:80/with/path", "ftp://:****@ported.url:80/with/path",
			"ftp://user:@normal.url", "ftp://user:****@normal.url",
			"ftp://user:@normal.url/with/path", "ftp://user:****@normal.url/with/path",
			"ftp://user:@ported.url:80", "ftp://user:****@ported.url:80",
			"ftp://user:@ported.url:80/with/path", "ftp://user:****@ported.url:80/with/path",
			"ftp://:pass@normal.url", "ftp://:****@normal.url",
			"ftp://:pass@normal.url/with/path", "ftp://:****@normal.url/with/path",
			"ftp://:pass@ported.url:80", "ftp://:****@ported.url:80",
			"ftp://:pass@ported.url:80/with/path", "ftp://:****@ported.url:80/with/path",
			"ftp://user:pass@normal.url", "ftp://user:****@normal.url",
			"ftp://user:pass@normal.url/with/path", "ftp://user:****@normal.url/with/path",
			"ftp://user:pass@ported.url:80", "ftp://user:****@ported.url:80",
			"ftp://user:pass@ported.url:80/with/path", "ftp://user:****@ported.url:80/with/path",
		};

		for(int i=0; i<testData.length; i+=2) {
			String initial = testData[i];

			String expected = testData[i+1];
			if(expected == null) expected = initial;

			assertEquals(expected, SimpleJsonClient2.redactUrl(initial));
		}
	}
}
