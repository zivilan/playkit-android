package com.kaltura.playkit.backend;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.kaltura.playkit.BaseTest;
import com.kaltura.playkit.OnCompletion;
import com.kaltura.playkit.PKLog;
import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.PKMediaSource;
import com.kaltura.playkit.backend.base.OnMediaLoadCompletion;
import com.kaltura.playkit.backend.ovp.KalturaOvpMediaProvider;
import com.kaltura.playkit.backend.ovp.KalturaOvpParser;
import com.kaltura.playkit.backend.ovp.OvpSessionProvider;
import com.kaltura.playkit.backend.ovp.data.PrimitiveResult;
import com.kaltura.playkit.connect.Accessories;
import com.kaltura.playkit.connect.ErrorElement;
import com.kaltura.playkit.connect.RequestElement;
import com.kaltura.playkit.connect.RequestQueue;
import com.kaltura.playkit.connect.ResponseElement;
import com.kaltura.playkit.connect.ResultElement;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by tehilarozin on 10/11/2016.
 */

@RunWith(AndroidJUnit4.class)
public class OvpMediaProviderAndroidTest extends BaseTest {

    public static final String BaseUrl = "http://www.kaltura.com/api_v3/"; //login at: http://kmc.kaltura.com/index.php/kmc/kmc4#content|manage

    // Demo account user
    public static final String NonDRMEntryIdAnm = "1_25q88snr"; //works for anonymous
    public static final int NonDRMEntryIdAnmDuration = 167000;

    public static final String NonDRMEntryId = "1_xay0wjby"; //works for user/anonymous
    public static final int NonDRMEntryIdDuration = 96000;

    public static final String DRMEntryIdAnm = "1_ytsd86sc"; //works for anonymous //1_3wzacuha
    public static final int DRMEntryIdAnmDuration = 102000;

    public static final String DRMEntryIdUsr = "1_tmomdals"; //works for logged user
    public static final int DRMEntryIdUsrDuration = 30094;

    //public static final String DRMEntryIdUsr2 = "1_i02uprfp"; //works for user

    public static final String RestrictedEntryId = "1_3wzacuha"; //restricted with drm not working with kaltura.fe
    public static final String NotFoundEntryId = "0_tb83i9pr"; //should get error - not found
    public static final String MockEmptyEntryId = "0_5huwy2pz"; //should get error - empty content
    public static final String MockMsgsEntryId = "0_q4nkfriz"; //should get error - has restriction

    public static final int PartnerId = 2222401;
    public static final String LoginId = "kaltura.fe@icloud.com";
    public static final String Password = "abcd1234*";
    public static final String AnonymousKS = "djJ8MjIyMjQwMXzXI4NeVu8er1kyU5oUr9CQfR79mb3mpSxSnRM99MaITqbLMQMmATdEhAyESU7-IW7YxYwDdHvd2XPz7xVVCaV1y1fIx34NM69w9pJLcrZPiw==";



    RequestQueue testExecutor;
    KalturaOvpMediaProvider kalturaOvpMediaProvider;


    SessionProvider ksSessionProvider = new SessionProvider() {
        @Override
        public String baseUrl() {
            return BaseUrl;
        }

        @Override
        public void getSessionToken(OnCompletion<PrimitiveResult> completion) {
            if (completion != null) {
                completion.onComplete(new PrimitiveResult(AnonymousKS));
            }
        }

        @Override
        public int partnerId() {
            return PartnerId;
        }
    };

    /*SessionProvider qaSessionProvider = new SessionProvider() {
        @Override
        public String baseUrl() {
            return QABaseUrl;
        }

        @Override
        public void getSessionToken(OnCompletion<PrimitiveResult> completion) {
            if (completion != null) {
                completion.onComplete(new PrimitiveResult(QAKS));
            }
        }

        @Override
        public int partnerId() {
            return QAPartnerId;
        }
    };*/


    public OvpMediaProviderAndroidTest() {
        super("OvpMediaProviderAndroidTest");
    }

    @Before
    public void setUp() {
        testExecutor = new Executor();
    }

    @Test
    public void testAnonymousSessionEntryInfoWithDrmFetch() {
        final OvpSessionProvider sessionProvider = new OvpSessionProvider(BaseUrl);
        final AtomicReference<AssertionError> failure = new AtomicReference<>();

        sessionProvider.startAnonymousSession(PartnerId, new OnCompletion<PrimitiveResult>() {
            @Override
            public void onComplete(PrimitiveResult response) {
                if (response.error == null) {
                    loadMediaByEntryId(DRMEntryIdAnm, DRMEntryIdAnmDuration, 2, sessionProvider, failure, new TestBlock<ResultElement<PKMediaEntry>>() {
                        @Override
                        public void execute(ResultElement<PKMediaEntry> data) throws AssertionError{
                            PKMediaSource firstSource = data.getResponse().getSources().get(0);
                            assertNotNull(firstSource.getDrmData());
                            assertTrue(firstSource.getDrmData().size() == 2);
                            assertTrue(firstSource.getUrl().endsWith("mpd"));

                            PKMediaSource secondSource = data.getResponse().getSources().get(1);
                            Assert.assertTrue(secondSource.getDrmData().size() == 0);
                            assertTrue(secondSource.getUrl().endsWith("m3u8"));
                        }
                    });
                } else {
                    fail("failed to establish session: " + response.error);
                }
            }
        });

        wait(1);

        if (failure.get() != null) {
            throw failure.get();
        }
    }

    @Test
    public void testEntryInfoWithDrmFetch() {
        final OvpSessionProvider sessionProvider = new OvpSessionProvider(BaseUrl);
        final AtomicReference<AssertionError> failure = new AtomicReference<>();

        sessionProvider.startSession(LoginId, Password, PartnerId, new OnCompletion<PrimitiveResult>() {
            @Override
            public void onComplete(PrimitiveResult response) {
                if (response.error == null) {
                    loadMediaByEntryId(DRMEntryIdUsr, DRMEntryIdUsrDuration, 2, sessionProvider, failure, new TestBlock<ResultElement<PKMediaEntry>>() {
                        @Override
                        public void execute(ResultElement<PKMediaEntry> data) {
                            PKMediaSource firstSource = data.getResponse().getSources().get(0);
                            assertNotNull(firstSource.getDrmData());
                            assertTrue(firstSource.getDrmData().size() == 2);
                            assertTrue(firstSource.getUrl().endsWith("mpd"));

                            PKMediaSource secondSource = data.getResponse().getSources().get(1);
                            Assert.assertTrue(secondSource.getDrmData().size() == 0);
                            assertTrue(secondSource.getUrl().endsWith("m3u8"));
                        }
                    });
                } else {
                    fail("failed to establish session: " + response.error);
                }
            }
        });

        wait(1);

        if (failure.get() != null) {
            throw failure.get();
        }
    }

    @Test
    public void testEntryInfoSuccessFetch() {
        final OvpSessionProvider sessionProvider = new OvpSessionProvider(BaseUrl);
        final AtomicReference<AssertionError> failure = new AtomicReference<>();

        sessionProvider.startSession(LoginId, Password, PartnerId, new OnCompletion<PrimitiveResult>() {
            @Override
            public void onComplete(PrimitiveResult response) {
                if (response.error == null) {
                    loadMediaByEntryId(NonDRMEntryId, NonDRMEntryIdDuration, 2, sessionProvider, failure, new TestBlock<ResultElement<PKMediaEntry>>() {
                        @Override
                        public void execute(ResultElement<PKMediaEntry> data) {
                            PKMediaSource firstSource = data.getResponse().getSources().get(0);
                            assertTrue(firstSource.getDrmData().size() == 0);
                            assertTrue(firstSource.getUrl().endsWith("mpd"));

                            PKMediaSource secondSource = data.getResponse().getSources().get(1);
                            assertTrue(secondSource.getDrmData().size() == 0);
                            assertTrue(secondSource.getUrl().endsWith("m3u8"));
                        }
                    });

                } else {
                    fail("failed to establish session: " + response.error);
                }
            }
        });

        wait(1);

        if (failure.get() != null) {
            throw failure.get();
        }
    }


    private void loadMediaByEntryId(final String entryId, final int expectedDuration, final int expectedSrcsCount, OvpSessionProvider sessionProvider, final AtomicReference<AssertionError> failure, final TestBlock<ResultElement<PKMediaEntry>> testBlock) {
        new KalturaOvpMediaProvider().setSessionProvider(sessionProvider).setEntryId(entryId).load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                try {
                    if (response.isSuccess()) {
                        PKLog.d(TAG, "got PKMediaEntry object: checking content");
                        assertTrue(response.getResponse() != null);
                        assertTrue(response.getResponse().getId().equals(entryId));
                        assertTrue(response.getResponse().getSources().size() == expectedSrcsCount);
                        assertTrue(response.getResponse().getDuration() == expectedDuration);
                        PKLog.i(TAG, "PKMediaEntry validated successfully");

                    } else {
                        assertNotNull(response.getError());
                        PKLog.d(TAG, "got error on PKMediaEntry loading:" + response.getError());
                        fail("failed on entry loading:" + response.getError());
                    }

                    if (testBlock != null) {
                        testBlock.execute(response);
                    }

                } catch (AssertionError e) {
                    failure.set(e);
                    fail("failed on entry validation:" + e.getMessage());
                } finally {
                    OvpMediaProviderAndroidTest.this.resume();
                }
            }
        });
    }

    /**
     * expected - failure since entry should not be found
     */
    @Test
    public void testEntryInfoFailedFetch() {
        final OvpSessionProvider sessionProvider = new OvpSessionProvider(BaseUrl);
        final AtomicReference<AssertionError> failure = new AtomicReference<>();

        sessionProvider.startSession(LoginId, Password, PartnerId, new OnCompletion<PrimitiveResult>() {
            @Override
            public void onComplete(PrimitiveResult response) {
                if (response.error == null) {

                    new KalturaOvpMediaProvider().setSessionProvider(sessionProvider).setEntryId(NotFoundEntryId).load(new OnMediaLoadCompletion() {
                        @Override
                        public void onComplete(ResultElement<PKMediaEntry> response) {
                            try {
                                assertNotNull(response);
                                assertNotNull(response.getError());
                                assertTrue(response.getError().getCode().equals("INVALID_ENTRY_ID") || response.getError().getCode().equals("ENTRY_ID_NOT_FOUND"));

                            } catch (AssertionError e) {
                                failure.set(e);
                                fail("failed on entry validation:" + e.getMessage());
                            } finally {
                                OvpMediaProviderAndroidTest.this.resume();
                            }
                        }
                    });

                } else {
                    fail("failed to establish session: " + response.error);
                }
            }
        });
        wait(1);
        if (failure.get() != null) {
            throw failure.get();
        }
    }

    @Test
    public void testEntryInfoWithMessagesFetch() {
        final OvpSessionProvider sessionProvider = new OvpSessionProvider(BaseUrl);
        final AtomicReference<AssertionError> failure = new AtomicReference<>();

        sessionProvider.startAnonymousSession(PartnerId, new OnCompletion<PrimitiveResult>() {
            @Override
            public void onComplete(PrimitiveResult response) {
                if (response.error == null) {

                    new KalturaOvpMediaProvider().setSessionProvider(sessionProvider).setEntryId(RestrictedEntryId).load(new OnMediaLoadCompletion() {
                        @Override
                        public void onComplete(ResultElement<PKMediaEntry> response) {
                            try {
                                assertNotNull(response);
                                assertNotNull(response.getError());
                                Assert.assertNull(response.getResponse());
                                assertTrue(response.getError().equals(ErrorElement.RestrictionError));
                                PKLog.i(TAG, "Anonymous user got restriction error: " + response.getError());

                            } catch (AssertionError e) {
                                failure.set(e);
                                fail("response should have return an error");
                            } finally {
                                OvpMediaProviderAndroidTest.this.resume();
                            }
                        }
                    });

                } else {
                    fail("failed to establish session: " + response.error);
                }
            }
        });
        wait(1);
        if (failure.get() != null) {
            throw failure.get();
        }
    }

    @Test
    public void testQALoadRequest() {

        kalturaOvpMediaProvider = new KalturaOvpMediaProvider().setSessionProvider(ksSessionProvider).setEntryId(MockMsgsEntryId).setRequestExecutor(testExecutor);
        final AtomicReference<AssertionError> failure = new AtomicReference<>();

        kalturaOvpMediaProvider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                PKLog.d(TAG, "response " + response);

                try {
                    assertNotNull(response);
                    if (!response.isSuccess()) {
                        assertNotNull(response.getError());
                        PKLog.w(TAG, "Content can't be played:\n" + response.getError());

                    } else {
                        assertNotNull(response.getResponse());
                        assertTrue(response.getResponse().getId().equals(MockMsgsEntryId));
                        assertTrue(response.getResponse().getSources().size() == 2);
                        assertTrue(response.getResponse().getDuration() == 136000);
                    }

                } catch (AssertionError e) {
                    failure.set(e);
                    fail("failed to assert response:" + e.getMessage());
                } finally {
                    OvpMediaProviderAndroidTest.this.resume();
                }
            }
        });
        wait(1);
        if (failure.get() != null) {
            throw failure.get();
        }
    }

    @Test
    public void testEmptyResponseRequest() {

        kalturaOvpMediaProvider = new KalturaOvpMediaProvider().setSessionProvider(ksSessionProvider).setEntryId(MockEmptyEntryId).setRequestExecutor(testExecutor);
        final AtomicReference<AssertionError> failure = new AtomicReference<>();

        kalturaOvpMediaProvider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                PKLog.d(TAG, "response " + response);

                try {
                    assertNotNull(response);
                    assertNotNull(response.getError());
                    PKLog.w(TAG, "Content can't be played:\n" + response.getError());

                } catch (AssertionError e) {
                    failure.set(e);
                    fail("failed to assert response:" + e.getMessage());
                } finally {
                    OvpMediaProviderAndroidTest.this.resume();
                }
            }
        });
        wait(1);
        if (failure.get() != null) {
            throw failure.get();
        }
    }

    @Test

    public void testCancelRequest() {

        final OvpSessionProvider sessionProvider = new OvpSessionProvider(BaseUrl);
        final AtomicReference<AssertionError> failure = new AtomicReference<>();
        final KalturaOvpMediaProvider mediaProvider = new KalturaOvpMediaProvider().setSessionProvider(sessionProvider).setEntryId(NonDRMEntryId);

        sessionProvider.startSession(LoginId, Password, PartnerId, new OnCompletion<PrimitiveResult>() {
            @Override
            public void onComplete(PrimitiveResult response) {
                if (response.error == null) {

                    mediaProvider.load(new OnMediaLoadCompletion() {
                        @Override
                        public void onComplete(ResultElement<PKMediaEntry> response) {
                            failure.set(new AssertionError("cancel didn't work"));
                            fail("should have been canceled");
                        }
                    });
                    OvpMediaProviderAndroidTest.this.resume();

                } else {
                    OvpMediaProviderAndroidTest.this.resume();

                }
            }
        });
        wait(1);
        try {
            TimeUnit.MILLISECONDS.sleep(620); // can be used to check cancel in different points of execution
            // mostly 625 milliseconds and up resulted in request finished execution, before the cancel was activated.
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mediaProvider.cancel();
        try {
            TimeUnit.SECONDS.sleep(8); // to make sure all callbacks finished - if executed
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertTrue(failure.get() == null);
    }

    @Test
    public void testPrimitiveResponseParsing() {
        String response = "true"; //2xUzNQszbmeucM9b_kAUUwT_0pvwxB4=
        //!! problem parsing "="charachter, causing malform json exception
        // -> we need to be able to parse it for the login request, we won't use parser mechanism in this case.
        String loginResponse = "djJ8MjIwOTU5MXzsuioBfhcT5p9oIFv3BN2fXlwzC9x1A1FEZCBS1gSG4e5eQYNwhMBm2t0Ooj4h0QeYjTpQZlCInCSkSk3dLf2sRmq0joAKH8Z32xUzNQszbmeucM9b_kAUUwT_0pvwxB4=";
        //String loginResponse = "{\"ks\":\"djJ8MjIwOTU5MXzsuioBfhcT5p9oIFv3BN2fXlwzC9x1A1FEZCBS1gSG4e5eQYNwhMBm2t0Ooj4h0QeYjTpQZlCInCSkSk3dLf2sRmq0joAKH8Z32xUzNQszbmeucM9b_kAUUwT_0pvwxB4=\"}";
        String loginRequestError = "{\n" +
                "  \"code\": \"USER_WRONG_PASSWORD\",\n" +
                "  \"message\": \"Wrong password supplied\",\n" +
                "  \"objectType\": \"KalturaAPIException\",\n" +
                "  \"args\": []\n" +
                "}";

        Object parsed = KalturaOvpParser.parse(response);
        assertTrue(parsed instanceof String);
        assertTrue(Boolean.TRUE.toString().equals(response));

        /*JsonReader reader = new JsonReader(new StringReader(loginResponse));
        reader.setLenient(true);
        parsed = KalturaOvpParser.parse(loginResponse);
        assertTrue(parsed instanceof String);
        assertTrue(((String)parsed).length() > 0);*/

        parsed = KalturaOvpParser.parse(loginRequestError);
        assertTrue(parsed instanceof BaseResult);
        assertTrue(((BaseResult) parsed).error != null);
        assertTrue(((BaseResult) parsed).error.getCode().equals("USER_WRONG_PASSWORD"));
    }

    /**
     * mock executor that reads precreated files that includes the mediaAsset/get response as if retrieved
     * from the server.
     * the mock response file name is constructed from the request and parameters.
     * [phoenix.serviceName.actionName.assetId.json]
     */
    class Executor implements RequestQueue {

        @Override
        public String queue(RequestElement request) {
            new RequestHandler(request).run();
            return null;
        }

        @Override
        public ResponseElement execute(RequestElement request) {
            new RequestHandler(request).run();
            return null;
        }

        @Override
        public void cancelRequest(String reqId) {

        }

        @Override
        public void clearRequests() {

        }

        @Override
        public boolean isEmpty() {
            return false;
        }


        class RequestHandler extends Thread {

            public static final String SERVICE = "/service/";
            public static final String ACTION = "/action/";
            private final RequestElement request;

            RequestHandler(RequestElement request) {
                this.request = request;
            }

            @Override
            public void run() {
                if (request != null) {
                    String url = request.getUrl();
                    int serviceIdx = url.indexOf(SERVICE);
                    int actionIdx = url.indexOf(ACTION);
                    String service = actionIdx == -1 ? url.substring(serviceIdx + SERVICE.length()) : url.substring(serviceIdx + SERVICE.length(), actionIdx);
                    String action = actionIdx == -1 ? "_" : url.substring(actionIdx + ACTION.length());

                    if (request.getBody() != null) {
                        JsonParser parser = new JsonParser();
                        JsonObject body = parser.parse(request.getBody()).getAsJsonObject();
                        String assetId = "";
                        if (body.has("2")) {
                            assetId = body.get("2").getAsJsonObject().getAsJsonPrimitive("entryId").getAsString();
                        }

                        //String assetId = NotFoundEntryId;//body.getAsJsonObject().getAsJsonPrimitive("id").getAsString();

                        String inputFile = "mock/ovp." + service + "." + action + "." + assetId + ".json";

                        try {
                            final JsonReader jsonReader = new JsonReader(new InputStreamReader(InstrumentationRegistry.getTargetContext().getAssets().open(inputFile)));


                            StringBuilder stringBuilder = new StringBuilder();
                            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(InstrumentationRegistry.getTargetContext().getAssets().open(inputFile)));//new FileReader(inputFile));
                            try {
                                String line = bufferedReader.readLine();
                                while (line != null) {
                                    stringBuilder.append(line);
                                    line = bufferedReader.readLine();
                                }

                            } catch (IOException ex) {
                            } finally {
                                bufferedReader.close();
                            }

                            request.onComplete(Accessories.buildResponse(stringBuilder.toString(), null));

                        } catch (IOException e) {
                            e.printStackTrace();
                            request.onComplete((ResponseElement) Accessories.<String>buildResult(null, ErrorElement.LoadError));
                        }


                    }

                }
            }
        }
    }
}
