package com.hcaptcha.sdk;

import static com.hcaptcha.sdk.AssertUtil.failAsNonReachable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.test.core.app.ActivityScenario;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hcaptcha.sdk.test.TestActivity;

import org.junit.After;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class HCaptchaWebViewHelperTest {
    private static final long AWAIT_CALLBACK_MS = 5000;

    private ActivityScenario<TestActivity> scenario;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final TestObject testObject = new TestObject();
    private HCaptchaWebViewHelper helper;

    final HCaptchaConfig baseConfig = HCaptchaConfig.builder()
            .siteKey("10000000-ffff-ffff-ffff-000000000001")
            .hideDialog(true)
            .tokenExpiration(1)
            .build();

    final HCaptchaInternalConfig internalConfig = HCaptchaInternalConfig.builder()
            .htmlProvider(new HCaptchaTestHtml())
            .build();

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.close();
        }
    }

    private void launchWebView(HCaptchaConfig config, IHCaptchaVerifier verifier, Runnable onPageFinished) {
        scenario = ActivityScenario.launch(TestActivity.class);
        scenario.onActivity(activity -> {
            HCaptchaWebView webView = new HCaptchaWebView(activity);
            webView.addJavascriptInterface(testObject, "TestObject");
            helper = new HCaptchaWebViewHelper(
                    handler, activity, config, internalConfig, verifier, webView);

            activity.setContentView(webView);
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    if (onPageFinished != null) {
                        onPageFinished.run();
                    }
                }
            });
        });
    }

    private void launchWebView(HCaptchaConfig config, IHCaptchaVerifier verifier) {
        launchWebView(config, verifier, null);
    }

    private void launchWebView(Runnable onPageFinished) {
        launchWebView(baseConfig, new TestIHCaptchaVerifier() {
            @Override
            public void onLoaded() {
                // Allow onLoaded to be called
            }
        }, onPageFinished);
    }

    private void launchWebView() {
        launchWebView(null);
    }

    @Test
    @Ignore("Host URI is now rejected; no insecure HTTP request is triggered. See issue #194.")
    public void testInsecureHttpRequestErrorHandling() throws Exception {
        Assume.assumeTrue("Skip test for release, because impossible to mock IHCaptchaVerifier", BuildConfig.DEBUG);

        final CountDownLatch failureLatch = new CountDownLatch(1);
        final HCaptchaConfig config = baseConfig.toBuilder().host("http://localhost").build();

        final IHCaptchaVerifier verifier = new TestIHCaptchaVerifier() {
            @Override
            public void onLoaded() {
                // Allow onLoaded to be called
            }

            @Override
            public void onFailure(HCaptchaException e) {
                assertEquals(HCaptchaError.INSECURE_HTTP_REQUEST_ERROR, e.getHCaptchaError());
                assertEquals("Insecure resource http://localhost/favicon.ico requested", e.getMessage());
                failureLatch.countDown();
            }
        };

        launchWebView(config, verifier);

        assertTrue(failureLatch.await(AWAIT_CALLBACK_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSetVerifyParamsWithPhoneNumber() throws Exception {
        launchWebView(() -> {
            final HCaptchaVerifyParams params = HCaptchaVerifyParams.builder()
                    .phoneNumber("+1234567890")
                    .build();
            helper.setVerifyParams(params);
        });

        // Wait for and get the parsed JSON
        HCaptchaVerifyParams receivedParams = testObject.waitForSetData();

        // Verify the phone number was passed correctly
        assertEquals("+1234567890", receivedParams.getPhoneNumber());
    }

    @Test
    public void testSetVerifyParamsWithNull() throws Exception {
        launchWebView(() -> {
            helper.setVerifyParams(null);
        });

        // Wait for and get the parsed JSON
        HCaptchaVerifyParams receivedParams = testObject.waitForSetData();

        // Verify null case is handled correctly
        assertEquals("Should return empty verify params for null parameters", new HCaptchaVerifyParams(), receivedParams);
    }

    @Test
    public void testSetVerifyParamsWithPhonePrefix() throws Exception {
        launchWebView(() -> {
            final HCaptchaVerifyParams params = HCaptchaVerifyParams.builder()
                    .phonePrefix("+1")
                    .build();
            helper.setVerifyParams(params);
        });

        // Wait for and get the parsed JSON
        HCaptchaVerifyParams receivedParams = testObject.waitForSetData();

        // Verify the phone prefix was passed correctly
        assertEquals("+1", receivedParams.getPhonePrefix());
    }

    @Test
    public void testSetVerifyParamsWithRqdata() throws Exception {
        launchWebView(() -> {
            final HCaptchaVerifyParams params = HCaptchaVerifyParams.builder()
                    .rqdata("test-rqdata-from-params")
                    .build();
            helper.setVerifyParams(params);
        });

        // Wait for and get the parsed JSON
        HCaptchaVerifyParams receivedParams = testObject.waitForSetData();

        // Verify the rqdata was passed correctly
        assertEquals("test-rqdata-from-params", receivedParams.getRqdata());
    }

    @Test
    public void testSetVerifyParamsWithRqdataFromConfig() throws Exception {
        final HCaptchaConfig configWithRqdata = baseConfig.toBuilder()
                .rqdata("test-rqdata-from-config")
                .build();

        launchWebView(configWithRqdata, new TestIHCaptchaVerifier() {
            @Override
            public void onLoaded() {
                // Allow onLoaded to be called
            }
        }, () -> {
            // Test with null params - should get rqdata from config
            helper.setVerifyParams(null);
        });

        // Wait for and get the parsed JSON
        HCaptchaVerifyParams receivedParams = testObject.waitForSetData();

        // Verify the rqdata from config was passed correctly
        assertEquals("test-rqdata-from-config", receivedParams.getRqdata());
    }

    @Test
    public void testSetVerifyParamsWithRqdataPriority() throws Exception {
        final HCaptchaConfig configWithRqdata = baseConfig.toBuilder()
                .rqdata("test-rqdata-from-config")
                .build();

        launchWebView(configWithRqdata, new TestIHCaptchaVerifier() {
            @Override
            public void onLoaded() {
                // Allow onLoaded to be called
            }
        }, () -> {
            // Test with params that have rqdata - should override config rqdata
            final HCaptchaVerifyParams params = HCaptchaVerifyParams.builder()
                    .rqdata("test-rqdata-from-params")
                    .build();
            helper.setVerifyParams(params);
        });

        // Wait for and get the parsed JSON
        HCaptchaVerifyParams receivedParams = testObject.waitForSetData();

        // Verify the rqdata from params has priority over config
        assertEquals("test-rqdata-from-params", receivedParams.getRqdata());
    }

    private static abstract class TestIHCaptchaVerifier implements IHCaptchaVerifier {
        @Override
        public void onOpen() {
            failAsNonReachable();
        }

        @Override
        public void onLoaded() {
            failAsNonReachable();
        }

        @Override
        public void startVerification(Activity activity, HCaptchaVerifyParams verifyParams) {
            failAsNonReachable();
        }

        @Override
        public void reset() {
            failAsNonReachable();
        }

        @Override
        public void onSuccess(String token) {
            failAsNonReachable();
        }

        @Override
        public void onFailure(HCaptchaException e) {
            failAsNonReachable();
        }
    }

    private static class TestObject {
        private final CountDownLatch setDataLatch = new CountDownLatch(1);
        private final ObjectMapper objectMapper = new ObjectMapper();
        private HCaptchaVerifyParams receivedParams;

        @JavascriptInterface
        public void setData(String jsonParams) {
            try {
                if (jsonParams != null && !jsonParams.equals("null")) {
                    this.receivedParams = objectMapper.readValue(jsonParams, HCaptchaVerifyParams.class);
                } else {
                    this.receivedParams = null;
                }
            } catch (Exception e) {
                this.receivedParams = null;
            }
            setDataLatch.countDown();
        }

        /**
         * Wait for setVerifyParams to be called and return the parsed JSON
         */
        public HCaptchaVerifyParams waitForSetData() throws InterruptedException {
            assertTrue("setData should be called within timeout",
                setDataLatch.await(AWAIT_CALLBACK_MS, TimeUnit.MILLISECONDS));
            return receivedParams;
        }
    }
}
