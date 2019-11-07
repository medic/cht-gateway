package medic.gateway.alert;

import org.junit.*;
import org.junit.runner.*;
import org.robolectric.*;
import org.robolectric.annotation.*;

//import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=26)
@SuppressWarnings({"PMD.ExcessivePublicCount", "PMD.GodClass", "PMD.JUnitTestsShouldIncludeAssert", "PMD.SignatureDeclareThrowsException", "PMD.TooManyMethods"})
public class WakefulServiceTest {
	@Test
	public void test_wakefulService_multiplePollsIfNeeded() throws Exception {
		WebappPoller mockPoller = mock(WebappPoller.class);
		WakefulService service = spy(new WakefulService());
		when(service.getWebappPoller()).thenReturn(mockPoller);

		when(mockPoller.moreMessagesToSend()).thenReturn(true).thenReturn(false);

		service.doWakefulWork(null);

		verify(mockPoller, times(2)).pollWebapp();
	}
}
