package com.hcaptcha.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.test.core.app.ActivityScenario;

import com.hcaptcha.sdk.test.TestActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(Parameterized.class)
public class HCaptchaJourneyParamsTest {
    private HCaptchaWebViewHelper helper;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public Object journey;

    @Parameterized.Parameter(2)
    public String expected;

    @Parameterized.Parameter(3)
    public boolean includeOtherParams;

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> cases() throws JSONException {
        final JSONArray arrayCycle = new JSONArray();
        arrayCycle.put(arrayCycle);
        final JSONObject objectCycle = new JSONObject();
        objectCycle.put("self", objectCycle);
        return Arrays.asList(new Object[][] {
                {"bytes", new byte[] {1, 2, 3}, "AQID", true},
                {"characters", new char[] {'a', 'b'}, "ab", true},
                {"explicit null", JSONObject.NULL, "null", false},
                {"array cycle", arrayCycle, null, true},
                {"object cycle", objectCycle, null, true},
                {"NaN", new JSONArray().put((Object) Double.NaN), null, true},
                {"POJO", new Object(), null, true},
                {"enum", Collections.singletonMap("theme", HCaptchaTheme.LIGHT), null, true},
                {"UUID", Collections.singletonMap("id", new UUID(0, 1)), null, true},
                {"invalid iterator", (Iterable<Object>) () -> {
                    throw new IllegalStateException();
                }, null, true},
                {"cycle only", arrayCycle, null, false},
                {"NaN only", new JSONArray().put((Object) Double.NaN), null, false}
        });
    }

    @Test
    public void paramsReachJavascriptWithInvalidJourneyOmitted() throws Exception {
        final Capture capture = new Capture();
        final AtomicReference<HCaptchaException> failure = new AtomicReference<>();
        final HCaptchaVerifyParams params = HCaptchaVerifyParams.builder().userJourney(journey).build();
        if (includeOtherParams) {
            params.setPhonePrefix("44");
            params.setPhoneNumber("+44123456789");
            params.setRqdata("request-data");
        }
        final HCaptchaConfig config = HCaptchaConfig.builder().siteKey("site-key").build();
        final HCaptchaInternalConfig internal = HCaptchaInternalConfig.builder().htmlProvider(() ->
                "<html><script>var params=null,executions=0;function reset(){}"
                        + "function setData(data){params=data;}function execute(){executions++;}</script></html>")
                .build();
        try (ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class)) {
            scenario.onActivity(activity -> {
                final HCaptchaWebView webView = new HCaptchaWebView(activity);
                webView.addJavascriptInterface(capture, "Capture");
                final IHCaptchaVerifier verifier = new IHCaptchaVerifier() {
                    @Override
                    public void onLoaded() { }

                    @Override
                    public void onOpen() { }

                    @Override
                    public void onSuccess(String token) { }

                    @Override
                    public void onFailure(HCaptchaException error) {
                        failure.set(error);
                    }

                    @Override
                    public void startVerification(Activity host, HCaptchaVerifyParams values) { }

                    @Override
                    public void reset() { }

                    @Override
                    public void destroy() { }

                    @Override
                    public void onWebViewGone(HCaptchaException error) {
                        failure.set(error);
                    }
                };
                helper = new HCaptchaWebViewHelper(
                        new Handler(Looper.getMainLooper()), activity, config, internal, verifier, webView);
                activity.setContentView(webView);
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        helper.resetAndExecute(params);
                        view.loadUrl("javascript:Capture.record(JSON.stringify({params:params,executions:executions}));");
                    }
                });
            });
            try {
                assertTrue("WebView should report the final state", capture.done.await(5, TimeUnit.SECONDS));
                final JSONObject result = new JSONObject(capture.json);
                assertNull(failure.get());
                assertEquals(1, result.getInt("executions"));
                if (!includeOtherParams && expected == null) {
                    assertTrue(result.isNull("params"));
                } else {
                    final JSONObject received = result.getJSONObject("params");
                    if (includeOtherParams) {
                        assertEquals("44", received.getString("mfa_phoneprefix"));
                        assertEquals("+44123456789", received.getString("mfa_phone"));
                        assertEquals("request-data", received.getString("rqdata"));
                    }
                    if (expected == null) {
                        assertEquals(3, received.length());
                        assertFalse(received.has("userjourney"));
                    } else {
                        assertEquals(expected, received.getString("userjourney"));
                    }
                }
            } finally {
                scenario.onActivity(activity -> helper.destroy());
            }
        }
    }

    private static class Capture {
        private final CountDownLatch done = new CountDownLatch(1);
        private String json;

        @JavascriptInterface
        public void record(String value) {
            json = value;
            done.countDown();
        }
    }
}
