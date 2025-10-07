package com.hcaptcha.sdk;

import static com.hcaptcha.sdk.AssertUtil.failAsNonReachable;
import static com.hcaptcha.sdk.AssertUtil.waitHCaptchaWebViewError;
import static com.hcaptcha.sdk.AssertUtil.waitHCaptchaWebViewToken;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.hcaptcha.sdk.test.TestActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
            .build();

    final HCaptchaInternalConfig internalConfig = HCaptchaInternalConfig.builder()
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
                failAsNonReachable();
            }
        };

        final ActivityScenario<TestActivity> scenario = rule.getScenario();
        scenario.onActivity(activity -> {
            final HCaptchaHeadlessWebView subject = new HCaptchaHeadlessWebView(
                    activity, config, internalConfig, listener);
            subject.startVerification(activity, null);
        });

        waitHCaptchaWebViewToken(latch, AWAIT_CALLBACK_MS);
    }

    @Test
    public void testFailure() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final HCaptchaStateListener listener = new HCaptchaStateTestAdapter() {

            @Override
            void onSuccess(String token) {
                failAsNonReachable();
            }

            @Override
            void onFailure(HCaptchaException exception) {
                latch.countDown();
            }
        };

        final ActivityScenario<TestActivity> scenario = rule.getScenario();
        scenario.onActivity(activity -> {
            final HCaptchaHeadlessWebView subject = new HCaptchaHeadlessWebView(
                    activity, config, internalConfig, listener);
            subject.startVerification(activity, null);
        });

        waitHCaptchaWebViewError(latch, HCaptchaError.ERROR, AWAIT_CALLBACK_MS);
    }

    @Test
    public void testRetryPredicate() throws Exception {
        final CountDownLatch failureLatch = new CountDownLatch(1);
        final CountDownLatch successLatch = new CountDownLatch(1);
        final HCaptchaStateListener listener = new HCaptchaStateTestAdapter() {

            @Override
            void onSuccess(String token) {
                successLatch.countDown();
            }

            @Override
            void onFailure(HCaptchaException exception) {
                failAsNonReachable();
            }
        };

        final HCaptchaConfig updatedWithRetry = config.toBuilder()
                .retryPredicate((c, e) -> {
                    failureLatch.countDown();
                    return e.getHCaptchaError() == HCaptchaError.NETWORK_ERROR;
                })
                .build();

        final ActivityScenario<TestActivity> scenario = rule.getScenario();
        scenario.onActivity(activity -> {
            final HCaptchaHeadlessWebView subject = new HCaptchaHeadlessWebView(
                    activity, updatedWithRetry, internalConfig, listener);
            subject.startVerification(activity, null);
        });

        waitHCaptchaWebViewError(failureLatch, HCaptchaError.NETWORK_ERROR, AWAIT_CALLBACK_MS);

        waitHCaptchaWebViewToken(successLatch, AWAIT_CALLBACK_MS);
    }

    @Test
    public void testNotRetryPredicate() throws Exception {
        final CountDownLatch failureLatch = new CountDownLatch(1);
        final CountDownLatch retryLatch = new CountDownLatch(1);
        final HCaptchaStateListener listener = new HCaptchaStateTestAdapter() {

            @Override
            void onSuccess(String token) {
                failAsNonReachable();
            }

            @Override
            void onFailure(HCaptchaException exception) {
                failureLatch.countDown();
            }
        };

        final HCaptchaConfig updatedWithRetry = config.toBuilder()
                .retryPredicate((c, e) -> {
                    retryLatch.countDown();
                    return e.getHCaptchaError() != HCaptchaError.NETWORK_ERROR;
                })
                .build();

        final ActivityScenario<TestActivity> scenario = rule.getScenario();
        scenario.onActivity(activity -> {
            final HCaptchaHeadlessWebView subject = new HCaptchaHeadlessWebView(
                    activity, updatedWithRetry, internalConfig, listener);
            subject.startVerification(activity, null);
        });

        waitHCaptchaWebViewError(retryLatch, HCaptchaError.NETWORK_ERROR, AWAIT_CALLBACK_MS);

        assertTrue(failureLatch.await(AWAIT_CALLBACK_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testReset() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final ActivityScenario<TestActivity> scenario = rule.getScenario();
        scenario.onActivity(activity -> {
            final HCaptchaHeadlessWebView subject = new HCaptchaHeadlessWebView(
                    activity, config, internalConfig, new HCaptchaStateTestAdapter());

            final ViewGroup rootView = (ViewGroup) activity.getWindow().getDecorView().getRootView();
            final View webView = rootView.findViewById(R.id.webView);
            webView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(@NonNull View view) {
                    // will not be fired because attached already
                }

                @Override
                public void onViewDetachedFromWindow(@NonNull View view) {
                    latch.countDown();
                }
            });
            subject.reset();
        });

        assertTrue(latch.await(AWAIT_CALLBACK_MS, TimeUnit.MILLISECONDS));
    }
}
