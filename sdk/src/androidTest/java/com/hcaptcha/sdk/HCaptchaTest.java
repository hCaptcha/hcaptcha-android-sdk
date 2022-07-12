package com.hcaptcha.sdk;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static androidx.test.espresso.web.model.Atoms.getCurrentUrl;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static com.hcaptcha.sdk.AssertUtil.evaluateJavascript;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.hcaptcha.sdk.tasks.OnFailureListener;
import com.hcaptcha.sdk.tasks.OnSuccessListener;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class HCaptchaTest {
    private static final long AWAIT_CALLBACK_MS = 5000;

    @Rule
    public ActivityScenarioRule<TestActivity> rule = new ActivityScenarioRule<>(TestActivity.class);

    final HCaptchaConfig config = HCaptchaConfig.builder()
            .siteKey("10000000-ffff-ffff-ffff-000000000001")
            .hideDialog(true)
            .expirationTimeout(1)
            .build();

    @Test
    public void testExpiredAfterSuccess() throws Exception {
        final CountDownLatch latch = new CountDownLatch(2);

        final ActivityScenario<TestActivity> scenario = rule.getScenario();
        scenario.onActivity(activity -> {
            HCaptcha.getClient(activity)
                    .verifyWithHCaptcha(config)
                    .addOnSuccessListener(new OnSuccessListener<HCaptchaTokenResponse>() {
                        @Override
                        public void onSuccess(HCaptchaTokenResponse response) {
                            latch.countDown();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(HCaptchaException exception) {
                            assertEquals(HCaptchaError.TOKEN_TIMEOUT, exception.getHCaptchaError());
                            latch.countDown();
                        }
                    });
        });

        onWebView().check(webMatches(getCurrentUrl(), containsString("hcaptcha-form.html")));
        onView(withId(R.id.webView)).perform(evaluateJavascript("onPass(\"some-token\")"));

        assertTrue(latch.await(AWAIT_CALLBACK_MS, TimeUnit.MILLISECONDS)); // wait for callback
    }

    @Test
    public void webViewSessionTimeoutSuppressed() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final ActivityScenario<TestActivity> scenario = rule.getScenario();
        scenario.onActivity(activity -> {
            HCaptcha.getClient(activity)
                    .verifyWithHCaptcha(config)
                    .addOnSuccessListener(new OnSuccessListener<HCaptchaTokenResponse>() {
                        @Override
                        public void onSuccess(HCaptchaTokenResponse response) {
                            response.markUsed();
                            latch.countDown();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(HCaptchaException exception) {
                            fail("Session timeout should not be happened");
                        }
                    });
        });

        onWebView().check(webMatches(getCurrentUrl(), containsString("hcaptcha-form.html")));
        onView(withId(R.id.webView)).perform(evaluateJavascript("onPass(\"some-token\")"));

        assertTrue(latch.await(AWAIT_CALLBACK_MS, TimeUnit.MILLISECONDS)); // wait for callback
    }
}
