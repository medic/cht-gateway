package medic.gateway;

import android.support.test.espresso.*;
import android.support.test.rule.*;
import android.support.test.runner.*;
import android.view.*;
import android.widget.*;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import medic.gateway.test.*;

import org.junit.*;
import org.junit.runner.*;

import static android.support.test.InstrumentationRegistry.*;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.*;
import static android.support.test.espresso.assertion.ViewAssertions.*;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static medic.gateway.R.*;
import static medic.gateway.test.DbTestHelper.*;
import static medic.gateway.test.TestUtils.*;
import static medic.gateway.test.InstrumentationTestUtils.*;

@RunWith(AndroidJUnit4.class)
@SuppressWarnings({"PMD.SignatureDeclareThrowsException", "PMD.JUnitTestsShouldIncludeAssert"})
public class SettingsDialogActivityTest {
	@Rule @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
	public ActivityTestRule<SettingsDialogActivity> activityTestRule =
			new ActivityTestRule<>(SettingsDialogActivity.class);

	private HttpTestHelper http;

	@Before
	public void setUp() throws Throwable {
		clearAppSettings();
		this.http = new HttpTestHelper();
		preventScreenLock(activityTestRule);
	}

	@After
	public void tearDown() throws Exception {
		clearAppSettings();
	}

	@Test
	public void shouldDisplayCancelButtonIfSettingsExist() throws Exception {
		// given
		settingsStore().save(new Settings(http.url(), true));

		// when
		recreateActivityFor(activityTestRule);

		// then
		onView(withId(id.btnCancelSettings))
				.check(matches(isDisplayed()));
	}

	@Test
	public void shouldNotDisplayCancelButtonIfSettingsDoNotExist() {
		// expect
		onView(withId(id.btnCancelSettings))
				.check(matches(not(isDisplayed())));
	}

	@Test
	public void leavingUrlBlankShouldShowError() {
		// given
		urlEnreredAs("");

		// when
		saveClicked();

		// then
		assertErrorDisplayed(string.errInvalidUrl);
	}

	@Test
	public void enteringInvalidUrlShouldShowError() {
		// given
		urlEnreredAs("nonsense");

		// when
		saveClicked();

		// then
		assertErrorDisplayed(string.errInvalidUrl);
	}

	@Test
	public void enteringUrlWhichDoesNotRespondShouldShowError() {
		// given
		urlEnreredAs("http://not-a-real-domain-i-hope.com");

		// when
		saveClicked();

		// then
		assertErrorDisplayed(string.errWebappUrl_serverNotFound);
	}

	@Test
	public void enteringUrlWhichRespondsIncorrectlyShouldShowError() {
		// given
		http.nextResponseJson("{ \"bad\": true }");
		urlEnreredAs(http.url());

		// when
		saveClicked();

		// then
		assertErrorDisplayed(string.errWebappUrl_appNotFound);
	}

	@Test
	public void enteringUrlWhichRespondsWithUnauthorisedShouldShowError() {
		// given
		http.nextResponseError(401);
		urlEnreredAs(http.url());

		// when
		saveClicked();

		// then
		assertErrorDisplayed(string.errWebappUrl_unauthorised);
	}

	@Test
	public void enteringGoodUrlShouldSaveWebappUrl() {
		// given
		assertFalse(settingsStore().hasSettings());
		http.nextResponseJson("{ \"medic-gateway\": true }");
		urlEnreredAs(http.url());

		// when
		saveClicked();

		// then
		SettingsStore s = settingsStore();
		assertTrue(settingsStore().hasSettings());
		assertEquals(http.url(), s.getWebappUrl());
	}

	@Test
	public void enteringGoodUrlShouldForwardToMessagesList() {
		// given
		http.nextResponseJson("{ \"medic-gateway\": true }");
		urlEnreredAs(http.url());

		// when
		saveClicked();

		// then
		assertVisible(id.btnRefreshLog);
	}

	@Test
	public void disablingPollingShouldSave() {
		// given
		assertFalse(settingsStore().hasSettings());
		http.nextResponseJson("{ \"medic-gateway\": true }");
		urlEnreredAs(http.url());
		uncheckPollingEnabled();

		// when
		saveClicked();

		// then
		SettingsStore s = settingsStore();
		assertTrue(settingsStore().hasSettings());
		assertFalse(s.isPollingEnabled());
	}

	@Test
	public void enablingPollingShouldSave() {
		// given
		assertFalse(settingsStore().hasSettings());
		http.nextResponseJson("{ \"medic-gateway\": true }");
		urlEnreredAs(http.url());
		checkPollingEnabled();

		// when
		saveClicked();

		// then
		SettingsStore s = settingsStore();
		assertTrue(settingsStore().hasSettings());
		assertTrue(s.isPollingEnabled());
	}

//> TEST HELPERS
	private void urlEnreredAs(String url) {
		onView(withId(id.txtWebappUrl))
				.perform(typeText(url), closeSoftKeyboard());
	}

	private void checkPollingEnabled() {
		onView(allOf(withId(id.cbxEnablePolling), isChecked()));
	}

	private void uncheckPollingEnabled() {
		onView(allOf(withId(id.cbxEnablePolling), isChecked()))
				.perform(click());
	}

	private void saveClicked() {
		onView(withId(id.btnSaveSettings))
				.perform(click());
	}

	private void assertErrorDisplayed(int errorMessageResourceId) {
		String errorString = getTargetContext().getResources().getString(errorMessageResourceId);
		assertErrorDisplayed(errorString);
	}

	private void assertErrorDisplayed(final String expectedMessage) {
		onView(withId(id.txtWebappUrl))
				.check(new ViewAssertion() {
					public void check(View view, NoMatchingViewException noViewFoundException) {
						if(!(view instanceof TextView))
							fail("Supplied view is not a TextView, so does not have an error property.");
						TextView tv = (TextView) view;
						assertEquals(expectedMessage, tv.getError());
					}
				});
	}

	private void assertVisible(int viewId) {
		onView(withId(viewId)).check(matches(isDisplayed()));
	}

	private SettingsStore settingsStore() {
		return SettingsStore.in(getTargetContext());
	}
}
