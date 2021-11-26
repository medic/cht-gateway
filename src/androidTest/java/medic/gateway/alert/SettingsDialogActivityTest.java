package medic.gateway.alert;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.*;
import androidx.test.filters.LargeTest;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import medic.gateway.alert.test.*;
import org.junit.*;
import org.junit.runner.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static medic.gateway.alert.BuildConfig.IS_MEDIC_FLAVOUR;
import static medic.gateway.alert.R.*;
import static medic.gateway.alert.test.InstrumentationTestUtils.*;

@LargeTest
@RunWith(AndroidJUnit4.class)
@SuppressWarnings({"PMD.SignatureDeclareThrowsException", "PMD.JUnitTestsShouldIncludeAssert", "PMD.GodClass", "PMD.TooManyMethods"})
public class SettingsDialogActivityTest {
	private static final boolean NOT_MEDIC_FLAVOUR = !IS_MEDIC_FLAVOUR;
	private static final boolean NOT_GENERIC_FLAVOUR = IS_MEDIC_FLAVOUR;

	@Rule @SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
	public ActivityScenarioRule<SettingsDialogActivity> activityTestRule =
			new ActivityScenarioRule<>(SettingsDialogActivity.class);

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
		assertVisible(id.cbxEnableCdmaCompatMode);
		assertVisible(id.btnSaveSettings);

		assertDoesNotExist(id.txtWebappInstanceName);
		assertDoesNotExist(id.txtWebappPassword);
	}

	@Test
	public void generic_shouldDisplayCancelButtonIfSettingsExist() throws Exception {
		if(NOT_GENERIC_FLAVOUR) /* test not applicable */ return;

		// given
		settingsStore().save(new Settings(http.url(), true, false, false));

		// when
		recreateActivityFor(activityTestRule);

		// then
		assertVisible(id.btnCancelSettings);
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

	@Test
	public void generic_cdmaCompat_shouldBeDisabledByDefault() {
		if(NOT_GENERIC_FLAVOUR) /* test not applicable */ return;

		// given
		assertFalse(settingsStore().hasSettings());
		http.nextResponseJson("{ \"medic-gateway\": true }");
		urlEnteredAs(http.url());
		assertNotChecked(id.cbxEnableCdmaCompatMode);

		// when
		saveClicked();

		// then
		assertTrue(settingsStore().hasSettings());
		assertFalse(settings().cdmaCompatMode);
	}

	@Test
	public void generic_cdmaCompat_shouldBeEnablable() {
		if(NOT_GENERIC_FLAVOUR) /* test not applicable */ return;

		// given
		assertFalse(settingsStore().hasSettings());
		http.nextResponseJson("{ \"medic-gateway\": true }");
		urlEnteredAs(http.url());
		checkCdmaCompatEnabled();

		// when
		saveClicked();

		// then
		assertTrue(settingsStore().hasSettings());
		assertTrue(settings().cdmaCompatMode);
	}

//> MEDIC FLAVOUR TESTS
	@Test
	public void medic_shouldDisplayCorrectFields() throws Exception {
		if(NOT_MEDIC_FLAVOUR) /* test not applicable */ return;

		// expect
		assertVisible(id.txtWebappInstanceName);
		assertVisible(id.txtWebappPassword);

		assertVisible(id.cbxEnablePolling);
		assertVisible(id.cbxEnableCdmaCompatMode);
		assertVisible(id.btnSaveSettings);

		assertDoesNotExist(id.txtWebappUrl);
	}

	@Test
	public void medic_shouldDisplayCancelButtonIfSettingsExist() throws Exception {
		if(NOT_MEDIC_FLAVOUR) /* test not applicable */ return;

		// given
		settingsStore().save(new Settings("https://uname:pword@test.dev.medicmobile.org/api/sms", true, false, false));

		// when
		recreateActivityFor(activityTestRule);

		// then
		assertVisible(id.btnCancelSettings);
	}

	@Test
	public void medic_shouldNotDisplayCancelButtonIfSettingsDoNotExist() {
		if(NOT_MEDIC_FLAVOUR) /* test not applicable */ return;

		// given
		clearAppSettings();

		// when
		recreateActivityFor(activityTestRule);

		// expect
		onView(withId(id.btnCancelSettings))
				.check(matches(not(isDisplayed())));
	}

	@Test
	public void medic_leavingWebappInstanceNameBlankShouldShowError() {
		if(NOT_MEDIC_FLAVOUR) /* test not applicable */ return;

		// given
		webappInstanceNameEnteredAs("");
		passwordEnteredAs("some-password");

		// when
		saveClicked();

		// then
		assertErrorDisplayed(string.errRequired);
	}

	@Test
	public void medic_leavingPasswordBlankShouldShowError() {
		if(NOT_MEDIC_FLAVOUR) /* test not applicable */ return;

		// given
		webappInstanceNameEnteredAs("some.instance");
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

	private void passwordEnteredAs(String password) {
		if(NOT_MEDIC_FLAVOUR) throw new IllegalStateException();

		enterText(id.txtWebappPassword, password);
	}

	private void checkPollingEnabled() {
		onView(allOf(withId(id.cbxEnablePolling), isChecked()));
	}

	private void uncheckPollingEnabled() {
		onView(allOf(withId(id.cbxEnablePolling), isChecked()))
				.perform(click());
	}

	private void checkCdmaCompatEnabled() {
		onView(allOf(withId(id.cbxEnableCdmaCompatMode), isNotChecked()))
				.perform(click());
	}

	private void saveClicked() {
		onView(withId(id.btnSaveSettings))
				.perform(click());
	}
}
