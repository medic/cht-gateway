package medic.gateway.alert;

import android.test.AndroidTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import android.content.Intent;
import com.commonsware.cwac.wakeful.WakefulIntentService;


import java.util.concurrent.TimeUnit;

import medic.gateway.alert.test.DbTestHelper;
import medic.gateway.alert.test.HttpTestHelper;
import okhttp3.mockwebserver.RecordedRequest;

import static medic.gateway.alert.test.DbTestHelper.cols;
import static medic.gateway.alert.test.DbTestHelper.randomUuid;
import static medic.gateway.alert.test.DbTestHelper.vals;
import static medic.gateway.alert.test.TestUtils.A_PHONE_NUMBER;
import static medic.gateway.alert.test.TestUtils.SOME_CONTENT;
import static medic.gateway.alert.WoMessage.Status.UNSENT;
import static org.junit.Assert.*;

public class WakefulServiceTest extends AndroidTestCase {

    private DbTestHelper db;
    private HttpTestHelper http;
    @Before
    public void setUp() throws Exception {
        super.setUp();

        db = new DbTestHelper(getContext());

        http = new HttpTestHelper();
        http.configureAppSettings(getContext());
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();

        http.tearDown();
        db.tearDown();

        http.assertNoMoreRequests();

    }

    @Test
    public void test_doWakefulWork_runPollWebAppAgainIfMessagesAvailable() throws Exception {
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
        Intent i = new Intent(getContext(), WakefulIntentService.class);
        WakefulService wfs = new WakefulService();
        wfs.doWakefulWork(i);

        //then
        db.assertEmpty("wo_message");
    }
}