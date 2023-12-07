package com.hcaptcha.sdk;

import static com.hcaptcha.sdk.AssertUtil.failAsNonReachable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import android.os.Handler;
import android.os.Looper;
import android.webkit.WebView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.hcaptcha.sdk.test.TestActivity;

import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class HCaptchaWebViewHelperTest {
    private static final long AWAIT_CALLBACK_MS = 5000;

    @Rule
    public ActivityScenarioRule<TestActivity> rule = new ActivityScenarioRule<>(TestActivity.class);

    final HCaptchaConfig baseConfig = HCaptchaConfig.builder()
            .siteKey("10000000-ffff-ffff-ffff-000000000001")
            .hideDialog(true)
            .tokenExpiration(1)
            .build();

    final HCaptchaInternalConfig internalConfig = HCaptchaInternalConfig.builder()
            .htmlProvider(new HCaptchaTestHtml())
            .build();

    @Test
    public void testInsecureHttpRequestErrorHandling() throws Exception {
        final Handler handler = new Handler(Looper.getMainLooper());
        final CountDownLatch failureLatch = new CountDownLatch(1);
        final HCaptchaConfig config = baseConfig.toBuilder().host("http://localhost").build();
        final IHCaptchaVerifier verifier = mock(IHCaptchaVerifier.class);

        final HCaptchaStateListener listener = new HCaptchaStateTestAdapter() {

            @Override
            void onSuccess(String token) {
                failAsNonReachable();
            }

            @Override
            void onFailure(HCaptchaException e) {
                assertEquals(HCaptchaError.INSECURE_HTTP_REQUEST_ERROR, e.getHCaptchaError());
                assertEquals("Insecure resource http://localhost/favicon.ico requested", e.getMessage());
                failureLatch.countDown();
            }
        };

        final ActivityScenario<TestActivity> scenario = rule.getScenario();
        scenario.onActivity(activity -> {
            WebView webView = new WebView(activity);
            final HCaptchaWebViewHelper helper = new HCaptchaWebViewHelper(
                    handler, activity, config, internalConfig, verifier, listener, webView);
        });

        assertTrue(failureLatch.await(AWAIT_CALLBACK_MS, TimeUnit.MILLISECONDS));
    }

}
