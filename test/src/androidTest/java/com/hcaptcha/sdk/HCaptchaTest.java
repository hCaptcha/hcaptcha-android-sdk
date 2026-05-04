package com.hcaptcha.sdk;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static com.hcaptcha.sdk.AssertUtil.waitHCaptchaWebViewToken;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.app.Activity;
import android.os.Looper;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.hcaptcha.sdk.tasks.OnSuccessListener;
import com.hcaptcha.sdk.test.TestActivity;
import com.hcaptcha.sdk.test.TestNonFragmentActivity;

import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class HCaptchaTest {
    private static final long AWAIT_CALLBACK_MS = 5000;
    private static final long E2E_AWAIT_CALLBACK_MS = AWAIT_CALLBACK_MS * 5;

    @Rule
    public ActivityScenarioRule<TestActivity> rule = new ActivityScenarioRule<>(TestActivity.class);

    final HCaptchaConfig config = HCaptchaConfig.builder()
            .siteKey("10000000-ffff-ffff-ffff-000000000001")
            .hideDialog(true)
            .tokenExpiration(1)
            .build();

    final HCaptchaInternalConfig internalConfig = HCaptchaInternalConfig.builder()
            .htmlProvider(new HCaptchaTestHtml())
            .build();

    @Test
    public void testExpiredAfterSuccess() throws Exception {
        final CountDownLatch latch = new CountDownLatch(2);

        final ActivityScenario<TestActivity> scenario = rule.getScenario();
        scenario.onActivity(activity -> HCaptcha.getClient(activity, internalConfig)
                .verifyWithHCaptcha(config)
                .addOnSuccessListener(response -> latch.countDown())
                .addOnFailureListener(exception -> {
                    assertEquals(HCaptchaError.TOKEN_TIMEOUT, exception.getHCaptchaError());
                    latch.countDown();
                }));

        waitHCaptchaWebViewToken(latch, AWAIT_CALLBACK_MS);
    }

    @Test
    public void webViewSessionTimeoutSuppressed() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final ActivityScenario<TestActivity> scenario = rule.getScenario();
        scenario.onActivity(activity -> HCaptcha.getClient(activity, internalConfig)
                .verifyWithHCaptcha(config)
                .addOnSuccessListener(response -> {
                    response.markUsed();
                    latch.countDown();
                })
                .addOnFailureListener(exception -> fail("Session timeout should not be happened")));

        waitHCaptchaWebViewToken(latch, AWAIT_CALLBACK_MS);
    }

    @Test
    public void removedListenerShouldNotBeCalled() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final OnSuccessListener<HCaptchaTokenResponse> listener1 = response -> {
            fail("Listener1 should never be called");
        };

        final OnSuccessListener<HCaptchaTokenResponse> listener2 = response -> {
            response.markUsed();
            latch.countDown();
        };

        final ActivityScenario<TestActivity> scenario = rule.getScenario();
        scenario.onActivity(activity -> HCaptcha.getClient(activity, internalConfig)
                .verifyWithHCaptcha(config)
                .addOnSuccessListener(listener1)
                .addOnFailureListener(exception -> fail("Session timeout should not be happened"))
                .removeOnSuccessListener(listener1)
                .addOnSuccessListener(listener2));

        waitHCaptchaWebViewToken(latch, AWAIT_CALLBACK_MS);
    }

    @Test
    public void e2eWithDebugTokenFragmentDialog() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final ActivityScenario<TestActivity> scenario = rule.getScenario();
        scenario.onActivity(activity -> HCaptcha.getClient(activity)
                .verifyWithHCaptcha(config.toBuilder().hideDialog(false).build())
                .addOnSuccessListener(response -> {
                    response.markUsed();
                    latch.countDown();
                })
                .addOnFailureListener(exception -> fail("No errors expected but received: " + exception.getHCaptchaError())));

        assertTrue(latch.await(E2E_AWAIT_CALLBACK_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void e2eWithDebugTokenHeadlessWebView() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final ActivityScenario<TestActivity> scenario = rule.getScenario();
        scenario.onActivity(activity -> HCaptcha.getClient(activity)
                .verifyWithHCaptcha(config.toBuilder().hideDialog(true).build())
                .addOnSuccessListener(response -> {
                    response.markUsed();
                    latch.countDown();
                })
                .addOnFailureListener(exception -> fail("No errors expected")));

        assertTrue(latch.await(E2E_AWAIT_CALLBACK_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void dialogVerifierReplaysLoadedAfterReset() throws Exception {
        final CountDownLatch firstSuccessLatch = new CountDownLatch(1);
        final CountDownLatch secondFailureLatch = new CountDownLatch(1);
        final AtomicReference<HCaptcha> hCaptchaRef = new AtomicReference<>();

        final HCaptchaInternalConfig resetAwareInternalConfig = internalConfig.toBuilder()
                .htmlProvider(new HCaptchaTestHtml(true, false, true))
                .build();
        final HCaptchaConfig dialogConfig = config.toBuilder().hideDialog(false).build();

        final ActivityScenario<TestActivity> scenario = rule.getScenario();
        scenario.onActivity(activity -> {
            final HCaptcha hCaptcha = HCaptcha.getClient(activity, resetAwareInternalConfig);
            hCaptchaRef.set(hCaptcha);
            hCaptcha.verifyWithHCaptcha(dialogConfig)
                    .addOnSuccessListener(response -> firstSuccessLatch.countDown())
                    .addOnFailureListener(exception -> {
                        if (firstSuccessLatch.getCount() == 0
                                && exception.getHCaptchaError() == HCaptchaError.ERROR) {
                            secondFailureLatch.countDown();
                        } else {
                            fail("Unexpected failure: " + exception.getHCaptchaError());
                        }
                    });
        });

        waitHCaptchaWebViewToken(firstSuccessLatch, AWAIT_CALLBACK_MS);
        getInstrumentation().waitForIdleSync();

        scenario.onActivity(activity -> hCaptchaRef.get().verifyWithHCaptcha(dialogConfig));

        assertTrue(secondFailureLatch.await(AWAIT_CALLBACK_MS, TimeUnit.MILLISECONDS));
    }

    @Test(expected = IllegalStateException.class)
    public void badActivity() {
        Looper.prepare();
        final Activity activity = new TestNonFragmentActivity();

        HCaptcha.getClient(activity)
                .verifyWithHCaptcha(config.toBuilder().hideDialog(false).diagnosticLog(true).build())
                .addOnSuccessListener(response -> fail("No token expected"))
                .addOnFailureListener(e -> fail("Wrong failure reason: " + e.getHCaptchaError()));
    }
}
