package medic.gateway.alert;

import android.test.AndroidTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import android.content.Intent;
import com.commonsware.cwac.wakeful.WakefulIntentService;

import medic.gateway.alert.test.DbTestHelper;
import medic.gateway.alert.test.HttpTestHelper;

import static medic.gateway.alert.WoMessage.Status.UNSENT;
import static medic.gateway.alert.test.DbTestHelper.cols;
import static medic.gateway.alert.test.DbTestHelper.randomUuid;
import static medic.gateway.alert.test.DbTestHelper.vals;
import static medic.gateway.alert.test.TestUtils.A_PHONE_NUMBER;
import static medic.gateway.alert.test.TestUtils.SOME_CONTENT;
import static medic.gateway.alert.WtMessage.Status.WAITING;

@SuppressWarnings({"PMD.SignatureDeclareThrowsException", "PMD.JUnitTestsShouldIncludeAssert"})
public class WakefulServiceTest extends AndroidTestCase {
	static class WakefulServiceMocked extends WakefulService{

		WakefulServiceMocked(){

			super();
		}

		protected Db getDbInstance(){

			return Db.getInstance(this);
		}

		protected WebappPoller getWebappPoller(){

			return new WebappPoller(getCtxInstance());
		}
	}

	private DbTestHelper db;
	private HttpTestHelper http;
	@Before
	public void setUp() throws Exception {
		super.setUp();

		db = new DbTestHelper(MedicGetwayApplication.getMedicApplicationContext());
		http = new HttpTestHelper();
		http.configureAppSettings(MedicGetwayApplication.getMedicApplicationContext());
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();

		http.tearDown();
		db.tearDown();

		http.assertNoMoreRequests();

	}
	@Test
	public void test_doWakefulWork_unsentMessagesShouldBeEmptyIfOnlyTenMessagesAvailable() throws Exception {
		// given
		db.insert("wo_message",
				cols("_id", "status",  "last_action", "_to",   "content"),
				vals(randomUuid(),  UNSENT,     0,     A_PHONE_NUMBER, SOME_CONTENT),
				vals(randomUuid(),  UNSENT,     0,     A_PHONE_NUMBER, SOME_CONTENT),
				vals(randomUuid(),  UNSENT,     0,     A_PHONE_NUMBER, SOME_CONTENT),
				vals(randomUuid(),  UNSENT,     0,     A_PHONE_NUMBER, SOME_CONTENT),
				vals(randomUuid(),  UNSENT,     0,     A_PHONE_NUMBER, SOME_CONTENT),
				vals(randomUuid(),  UNSENT,     0,     A_PHONE_NUMBER, SOME_CONTENT),
				vals(randomUuid(),  UNSENT,     0,     A_PHONE_NUMBER, SOME_CONTENT),
				vals(randomUuid(),  UNSENT,     0,     A_PHONE_NUMBER, SOME_CONTENT),
				vals(randomUuid(),  UNSENT,     0,     A_PHONE_NUMBER, SOME_CONTENT),
				vals(randomUuid(),  UNSENT,     0,     A_PHONE_NUMBER, SOME_CONTENT));
		http.nextResponseJson("{}");

		// when
		Intent i = new Intent(MedicGetwayApplication.getMedicApplicationContext(), WakefulIntentService.class);
		//WakefulService wfs = new WakefulServiceMocked();
		WakefulService wfs = new WakefulService(MedicGetwayApplication.getMedicApplicationContext());
		wfs.doWakefulWork(i);
		new WebappPoller(MedicGetwayApplication.getMedicApplicationContext()).pollWebapp();

		//then
		db.assertEmpty("wo_message");
	}

	public void test_doWakefulWork_unsentMessagesCountShouldBeEqualToTwoIfUnsentMessagesAreTwelve() throws Exception{
		// given
		db.insert("wo_message",
				cols("_id", "status",  "last_action", "_to",   "content"),
				vals(randomUuid(),  UNSENT,     0,     A_PHONE_NUMBER, SOME_CONTENT),
				vals(randomUuid(),  UNSENT,     0,     A_PHONE_NUMBER, SOME_CONTENT),
				vals(randomUuid(),  UNSENT,     0,     A_PHONE_NUMBER, SOME_CONTENT),
				vals(randomUuid(),  UNSENT,     0,     A_PHONE_NUMBER, SOME_CONTENT),
				vals(randomUuid(),  UNSENT,     0,     A_PHONE_NUMBER, SOME_CONTENT),
				vals(randomUuid(),  UNSENT,     0,     A_PHONE_NUMBER, SOME_CONTENT),
				vals(randomUuid(),  UNSENT,     0,     A_PHONE_NUMBER, SOME_CONTENT),
				vals(randomUuid(),  UNSENT,     0,     A_PHONE_NUMBER, SOME_CONTENT),
				vals(randomUuid(),  UNSENT,     0,     A_PHONE_NUMBER, SOME_CONTENT),
				vals(randomUuid(),  UNSENT,     0,     A_PHONE_NUMBER, SOME_CONTENT),
				vals(randomUuid(),  UNSENT,     0,     A_PHONE_NUMBER, SOME_CONTENT),
				vals(randomUuid(),  UNSENT,     0,     A_PHONE_NUMBER, SOME_CONTENT));
		http.nextResponseJson("{}");

		// when
		Intent i = new Intent(getContext(), WakefulIntentService.class);
		WakefulService wfs = new WakefulService(MedicGetwayApplication.getMedicApplicationContext());
		wfs.doWakefulWork(i);
		SimpleResponse response = new WebappPoller(MedicGetwayApplication.getMedicApplicationContext()).pollWebapp();

		//then
		db.assertCount("wo_message",2);
	}
}


