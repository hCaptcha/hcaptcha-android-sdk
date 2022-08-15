package com.hcaptcha.sdk;

import static com.hcaptcha.sdk.AssertUtil.waitHCaptchaWebViewError;
import static com.hcaptcha.sdk.AssertUtil.waitHCaptchaWebViewToken;
import static org.junit.Assert.fail;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

@RunWith(AndroidJUnit4.class)
public class HCaptchaHeadlessWebViewTest {
    private static final long AWAIT_CALLBACK_MS = 1000;

    @Rule
    public ActivityScenarioRule<TestActivity> rule = new ActivityScenarioRule<>(TestActivity.class);

    final HCaptchaConfig config = HCaptchaConfig.builder()
            .siteKey("10000000-ffff-ffff-ffff-000000000001")
            .loading(false)
            .size(HCaptchaSize.INVISIBLE)
            .hideDialog(true)
            .htmlProvider(new HCaptchaTestHtml())
            .build();

    @Test
    public void testSuccess() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final HCaptchaStateListener listener = new HCaptchaStateTestAdapter() {
            @Override
            void onOpen() {
                fail("Should never be called for HCaptchaHeadlessWebView");
            }

            @Override
            void onSuccess(String token) {
                latch.countDown();
            }

            @Override
            void onFailure(HCaptchaException exception) {
                fail("Should not be called for this test");
            }
        };

        final ActivityScenario<TestActivity> scenario = rule.getScenario();
        scenario.onActivity(activity -> {
            final HCaptchaHeadlessWebView subject = new HCaptchaHeadlessWebView(
                    activity, config, listener);
            subject.startVerification(activity);
        });

        waitHCaptchaWebViewToken(latch, AWAIT_CALLBACK_MS);
    }

    @Test
    public void testFailure() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final HCaptchaStateListener listener = new HCaptchaStateTestAdapter() {

            @Override
            void onSuccess(String token) {
                fail("Should not be called for this test");
            }

            @Override
            void onFailure(HCaptchaException exception) {
                latch.countDown();
            }
        };

        final ActivityScenario<TestActivity> scenario = rule.getScenario();
        scenario.onActivity(activity -> {
            final HCaptchaHeadlessWebView subject = new HCaptchaHeadlessWebView(
                    activity, config, listener);
            subject.startVerification(activity);
        });

        waitHCaptchaWebViewError(latch, HCaptchaError.ERROR, AWAIT_CALLBACK_MS);
    }
}
