package medic.gateway.alert;

import android.support.test.espresso.*;
import android.support.test.rule.*;
import android.support.test.runner.*;
import android.view.*;
import android.widget.*;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import medic.gateway.alert.test.*;

import org.junit.*;
import org.junit.runner.*;

import static android.support.test.InstrumentationRegistry.*;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.*;
import static android.support.test.espresso.assertion.ViewAssertions.*;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static medic.gateway.alert.BuildConfig.IS_MEDIC_FLAVOUR;
import static medic.gateway.alert.BuildConfig.TRAVIS;
import static medic.gateway.alert.R.*;
import static medic.gateway.alert.test.DbTestHelper.*;
import static medic.gateway.alert.test.TestUtils.*;
import static medic.gateway.alert.test.InstrumentationTestUtils.*;

@RunWith(AndroidJUnit4.class)
@SuppressWarnings({"PMD.SignatureDeclareThrowsException", "PMD.JUnitTestsShouldIncludeAssert", "PMD.GodClass", "PMD.TooManyMethods"})
public class SettingsDialogActivityTest {
	private static final boolean NOT_MEDIC_FLAVOUR = !IS_MEDIC_FLAVOUR;
	private static final boolean NOT_GENERIC_FLAVOUR = IS_MEDIC_FLAVOUR;

	@Rule @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
	public ActivityTestRule<SettingsDialogActivity> activityTestRule =
			new ActivityTestRule<>(SettingsDialogActivity.class);

	private HttpTestHelper http;

	@Before
	public void setUp() throws Throwable {
		clearAppSettings();
		this.http = new HttpTestHelper();
	}

	@After
	public void tearDown() throws Exception {
		clearAppSettings();
	}


//> GENERIC FLAVOUR TESTS
	@Test
	public void generic_shouldDisplayCorrectFields() throws Exception {
		if(NOT_GENERIC_FLAVOUR) /* test not applicable */ return;

		// expect
		assertVisible(id.txtWebappUrl);
		assertVisible(id.cbxEnablePolling);
		assertVisible(id.btnSaveSettings);

		assertDoesNotExist(id.txtWebappInstanceName);
		assertDoesNotExist(id.txtWebappUsername);
		assertDoesNotExist(id.txtWebappPassword);
	}

	@Test
	public void generic_shouldDisplayCancelButtonIfSettingsExist() throws Exception {
		if(NOT_GENERIC_FLAVOUR) /* test not applicable */ return;

		// given
		settingsStore().save(new Settings(http.url(), true));

		// when
		recreateActivityFor(activityTestRule);

		// then
		onView(withId(id.btnCancelSettings))
				.check(matches(isDisplayed()));
	}

	@Test
	public void generic_shouldNotDisplayCancelButtonIfSettingsDoNotExist() {
		if(NOT_GENERIC_FLAVOUR) /* test not applicable */ return;

		// expect
		onView(withId(id.btnCancelSettings))
				.check(matches(not(isDisplayed())));
	}

	@Test
	public void generic_leavingUrlBlankShouldShowError() {
		if(NOT_GENERIC_FLAVOUR) /* test not applicable */ return;

		// given
		urlEnteredAs("");

		// when
		saveClicked();

		// then
		assertErrorDisplayed(string.errRequired);
	}

	@Test
	public void generic_enteringInvalidUrlShouldShowError() {
		if(NOT_GENERIC_FLAVOUR) /* test not applicable */ return;

		// given
		urlEnteredAs("nonsense");

		// when
		saveClicked();

		// then
		assertErrorDisplayed(string.errInvalidUrl);
	}

	@Test
	public void generic_enteringUrlWhichDoesNotRespondShouldShowError() {
		if(NOT_GENERIC_FLAVOUR) /* test not applicable */ return;

		// given
		urlEnteredAs("http://not-a-real-domain-i-hope.com");

		// when
		saveClicked();

		// then
		assertErrorDisplayed(string.errWebappUrl_serverNotFound);
	}

	@Test
	public void generic_enteringUrlWhichRespondsIncorrectlyShouldShowError() {
		if(NOT_GENERIC_FLAVOUR) /* test not applicable */ return;

		// given
		http.nextResponseJson("{ \"bad\": true }");
		urlEnteredAs(http.url());

		// when
		saveClicked();

		// then
		assertErrorDisplayed(string.errWebappUrl_appNotFound);
	}

	@Test
	public void generic_enteringUrlWhichRespondsWithUnauthorisedShouldShowError() {
		if(NOT_GENERIC_FLAVOUR) /* test not applicable */ return;

		// given
		http.nextResponseError(401);
		urlEnteredAs(http.url());

		// when
		saveClicked();

		// then
		assertErrorDisplayed(string.errWebappUrl_unauthorised);
	}

	@Test
	public void generic_enteringGoodUrlShouldSaveWebappUrl() {
		if(NOT_GENERIC_FLAVOUR) /* test not applicable */ return;

		// given
		assertFalse(settingsStore().hasSettings());
		http.nextResponseJson("{ \"medic-gateway\": true }");
		urlEnteredAs(http.url());

		// when
		saveClicked();

		// then
		assertTrue(settingsStore().hasSettings());
		assertEquals(http.url(), settings().webappUrl);
	}

	@Test
	public void generic_disablingPollingShouldSave() {
		if(NOT_GENERIC_FLAVOUR) /* test not applicable */ return;

		// given
		assertFalse(settingsStore().hasSettings());
		http.nextResponseJson("{ \"error\": \"this url should not be contacted if polling disabled\" }");
		urlEnteredAs(http.url());
		uncheckPollingEnabled();

		// when
		saveClicked();

		// then
		assertTrue(settingsStore().hasSettings());
		assertFalse(settings().pollingEnabled);
	}

	@Test
	public void generic_enablingPollingShouldSave() {
		if(NOT_GENERIC_FLAVOUR) /* test not applicable */ return;

		// given
		assertFalse(settingsStore().hasSettings());
		http.nextResponseJson("{ \"medic-gateway\": true }");
		urlEnteredAs(http.url());
		checkPollingEnabled();

		// when
		saveClicked();

		// then
		assertTrue(settingsStore().hasSettings());
		assertTrue(settings().pollingEnabled);
	}

//> MEDIC FLAVOUR TESTS
	@Test
	public void medic_shouldDisplayCorrectFields() throws Exception {
		if(NOT_MEDIC_FLAVOUR) /* test not applicable */ return;

		// expect
		assertVisible(id.txtWebappInstanceName);
		assertVisible(id.txtWebappUsername);
		assertVisible(id.txtWebappPassword);

		assertVisible(id.cbxEnablePolling);
		assertVisible(id.btnSaveSettings);

		assertDoesNotExist(id.txtWebappUrl);
	}

	@Test
	public void medic_shouldDisplayCancelButtonIfSettingsExist() throws Exception {
		if(NOT_MEDIC_FLAVOUR) /* test not applicable */ return;
		if(TRAVIS) return; // TODO currently this test fails on travis CI.  It seems like this is due to a small screen size on the android emulator used by Travis

		// given
		settingsStore().save(new Settings("https://uname:pword@test.dev.medicmobile.org/api/sms", true));

		// when
		recreateActivityFor(activityTestRule);

		// then
		onView(withId(id.btnCancelSettings))
				.check(matches(isDisplayed()));
	}

	@Test
	public void medic_shouldNotDisplayCancelButtonIfSettingsDoNotExist() {
		if(NOT_MEDIC_FLAVOUR) /* test not applicable */ return;

		// expect
		onView(withId(id.btnCancelSettings))
				.check(matches(not(isDisplayed())));
	}

	@Test
	public void medic_leavingWebappInstanceNameBlankShouldShowError() {
		if(NOT_MEDIC_FLAVOUR) /* test not applicable */ return;

		// given
		webappInstanceNameEnteredAs("");
		usernameEnteredAs("some-user");
		passwordEnteredAs("some-password");

		// when
		saveClicked();

		// then
		assertErrorDisplayed(string.errRequired);
	}

	@Test
	public void medic_leavingUsernameBlankShouldShowError() {
		if(NOT_MEDIC_FLAVOUR) /* test not applicable */ return;

		// given
		webappInstanceNameEnteredAs("some.instance");
		usernameEnteredAs("");
		passwordEnteredAs("some-password");

		// when
		saveClicked();

		// then
		assertErrorDisplayed(id.txtWebappUsername, string.errRequired);
	}

	@Test
	public void medic_leavingPasswordBlankShouldShowError() {
		if(NOT_MEDIC_FLAVOUR) /* test not applicable */ return;

		// given
		webappInstanceNameEnteredAs("some.instance");
		usernameEnteredAs("some-user");
		passwordEnteredAs("");

		// when
		saveClicked();

		// then
		assertErrorDisplayed(id.txtWebappPassword, string.errRequired);
	}

	@Test
	public void medic_enteringBadInstanceNameShouldShowError() {
		if(NOT_MEDIC_FLAVOUR) /* test not applicable */ return;

		// given
		webappInstanceNameEnteredAs("...");
		usernameEnteredAs("user");
		passwordEnteredAs("pass");

		// when
		saveClicked();

		// then
		assertErrorDisplayed(string.errInvalidInstanceName);
	}

	@Test
	public void medic_disablingPollingShouldSave() {
		if(NOT_MEDIC_FLAVOUR) /* test not applicable */ return;

		// given
		assertFalse(settingsStore().hasSettings());
		webappInstanceNameEnteredAs("some.instance");
		usernameEnteredAs("some-user");
		passwordEnteredAs("some-password");
		uncheckPollingEnabled();

		// when
		saveClicked();

		// then
		assertTrue(settingsStore().hasSettings());
		assertFalse(settings().pollingEnabled);
	}

//> TEST HELPERS
	private void urlEnteredAs(String url) {
		if(NOT_GENERIC_FLAVOUR) throw new IllegalStateException();

		enterText(id.txtWebappUrl, url);
	}

	private void webappInstanceNameEnteredAs(String instanceName) {
		if(NOT_MEDIC_FLAVOUR) throw new IllegalStateException();

		enterText(id.txtWebappInstanceName, instanceName);
	}

	private void usernameEnteredAs(String username) {
		if(NOT_MEDIC_FLAVOUR) throw new IllegalStateException();

		enterText(id.txtWebappUsername, username);
	}

	private void passwordEnteredAs(String password) {
		if(NOT_MEDIC_FLAVOUR) throw new IllegalStateException();

		enterText(id.txtWebappPassword, password);
	}

	private void enterText(int componentId, String text) {
		onView(withId(componentId))
				.perform(typeText(text), closeSoftKeyboard());
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
		int componentId = IS_MEDIC_FLAVOUR ? id.txtWebappInstanceName : id.txtWebappUrl;
		assertErrorDisplayed(componentId, errorMessageResourceId);
	}

	private void assertErrorDisplayed(int componentId, int errorMessageResourceId) {
		String errorString = getTargetContext().getResources().getString(errorMessageResourceId);
		assertErrorDisplayed(componentId, errorString);
	}

	private void assertErrorDisplayed(int componentId, final String expectedMessage) {
		onView(withId(componentId))
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
		onView(withId(viewId)).perform(scrollTo()).check(matches(isDisplayed()));
	}

	@SuppressWarnings("PMD.EmptyCatchBlock")
	private void assertDoesNotExist(int viewId) {
		try {
			onView(withId(viewId)).check(matches(isDisplayed()));
			fail("Found view which should not exist!");
		} catch(NoMatchingViewException ex) {
			// expected
		}
	}

	private Settings settings() {
		return Settings.in(getTargetContext());
	}

	private SettingsStore settingsStore() {
		return SettingsStore.in(getTargetContext());
	}
}
