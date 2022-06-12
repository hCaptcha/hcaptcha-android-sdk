package com.hcaptcha.sdk;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static androidx.test.espresso.web.model.Atoms.getCurrentUrl;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static com.hcaptcha.sdk.AssertUtil.evaluateJavascript;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.webkit.WebView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


@RunWith(AndroidJUnit4.class)
public class HCaptchaHeadlessWebViewTest {
    @Rule
    public ActivityScenarioRule<TestActivity> rule = new ActivityScenarioRule<>(TestActivity.class);

    final HCaptchaConfig config = HCaptchaConfig.builder()
            .siteKey("10000000-ffff-ffff-ffff-000000000001")
            .loading(false)
            .size(HCaptchaSize.INVISIBLE)
            .build();

    @Test
    public void testSuccess() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final HCaptchaStateListener listener = new HCaptchaStateTestAdapter() {
            @Override
            void onOpen() {
                fail("Should never be called for HCaptchaHeadlessWebView");
            }

            @Override
            void onSuccess(HCaptchaTokenResponse response) {
                latch.countDown();
            }

            @Override
            void onFailure(HCaptchaException hCaptchaException) {
                fail("Should not be called for this test");
            }
        };

        final ActivityScenario<TestActivity> scenario = rule.getScenario();
        scenario.onActivity(activity -> {
            final HCaptchaHeadlessWebView subject = new HCaptchaHeadlessWebView(context, config, listener);
            subject.startVerification(activity);
        });

        onWebView().check(webMatches(getCurrentUrl(), containsString("hcaptcha-form.html")));
        onView(withId(R.id.webView)).perform(evaluateJavascript("onPass(\"some-token\")"));

        assertTrue(latch.await(1000, TimeUnit.MILLISECONDS)); // wait for callback
    }

    @Test
    public void testFailure() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final HCaptchaStateListener listener = new HCaptchaStateTestAdapter() {
            @Override
            void onOpen() {
                fail("Should never be called for HCaptchaHeadlessWebView");
            }

            @Override
            void onSuccess(HCaptchaTokenResponse response) {
                fail("Should not be called for this test");
            }

            @Override
            void onFailure(HCaptchaException hCaptchaException) {
                latch.countDown();
            }
        };

        final ActivityScenario<TestActivity> scenario = rule.getScenario();
        scenario.onActivity(activity -> {
            final HCaptchaHeadlessWebView subject = new HCaptchaHeadlessWebView(context, config, listener);
            subject.startVerification(activity);
        });

        onWebView().check(webMatches(getCurrentUrl(), containsString("hcaptcha-form.html")));
        onView(withId(R.id.webView)).perform(evaluateJavascript(
                "onError(" + HCaptchaError.ERROR.getErrorId() + ")"));

        assertTrue(latch.await(1000, TimeUnit.MILLISECONDS)); // wait for callback
    }
}
