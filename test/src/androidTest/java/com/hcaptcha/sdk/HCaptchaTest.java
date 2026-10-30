package com.hcaptcha.sdk;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static com.hcaptcha.sdk.AssertUtil.waitHCaptchaWebViewToken;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.app.Activity;
import android.os.Build;
import android.os.Looper;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebView;

import androidx.annotation.RequiresApi;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.SdkSuppress;

import com.hcaptcha.sdk.tasks.OnSuccessListener;
import com.hcaptcha.sdk.test.TestActivity;
import com.hcaptcha.sdk.test.TestNonFragmentActivity;

import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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

    // A dead renderer must not be reused: the next verify gets a token from a fresh WebView (#214)
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    public void headlessVerifyAfterRenderProcessGone() throws Exception {
        assertVerifyAfterRenderProcessGone(config);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    public void dialogVerifyAfterRenderProcessGone() throws Exception {
        assertVerifyAfterRenderProcessGone(config.toBuilder().hideDialog(false).build());
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void assertVerifyAfterRenderProcessGone(final HCaptchaConfig setupConfig) throws Exception {
        final CountDownLatch goneLatch = new CountDownLatch(1);
        final CountDownLatch successLatch = new CountDownLatch(1);
        final AtomicBoolean crashed = new AtomicBoolean(false);
        final AtomicReference<HCaptcha> hCaptchaRef = new AtomicReference<>();
        final AtomicReference<WebView> deadWebView = new AtomicReference<>();

        final ActivityScenario<TestActivity> scenario = rule.getScenario();
        scenario.onActivity(activity -> {
            final HCaptcha hCaptcha = HCaptcha.getClient(activity, internalConfig);
            hCaptchaRef.set(hCaptcha);
            hCaptcha.setup(setupConfig)
                    .addOnLoadedListener(() -> {
                        // Only the first WebView, the fresh one must survive
                        if (crashed.compareAndSet(false, true)) {
                            final WebView webView = webViewOf(hCaptcha);
                            deadWebView.set(webView);
                            // Not chrome://crash: all WebViews of the app share one renderer, so a real
                            // crash also kills the ones other tests left behind and fires their listeners
                            webView.getWebViewClient().onRenderProcessGone(webView, new CrashedRenderer());
                        }
                    })
                    .addOnSuccessListener(response -> {
                        response.markUsed();
                        successLatch.countDown();
                    })
                    .addOnFailureListener(exception -> {
                        assertEquals(HCaptchaError.ERROR, exception.getHCaptchaError());
                        assertEquals("WebView render process did terminate", exception.getMessage());
                        goneLatch.countDown();
                    });
        });
        assertTrue("render process should be reported gone",
                goneLatch.await(E2E_AWAIT_CALLBACK_MS, TimeUnit.MILLISECONDS));

        // No args, as documented after setup(config)
        scenario.onActivity(activity -> hCaptchaRef.get().verifyWithHCaptcha());

        waitHCaptchaWebViewToken(successLatch, AWAIT_CALLBACK_MS);
        scenario.onActivity(activity -> {
            final WebView freshWebView = webViewOf(hCaptchaRef.get());
            assertNotNull(deadWebView.get());
            assertNotSame("verify must not reuse the dead WebView", deadWebView.get(), freshWebView);
        });
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private static final class CrashedRenderer extends RenderProcessGoneDetail {
        @Override
        public boolean didCrash() {
            return true;
        }

        @Override
        public int rendererPriorityAtExit() {
            return WebView.RENDERER_PRIORITY_IMPORTANT;
        }
    }

    private static WebView webViewOf(final HCaptcha hCaptcha) {
        try {
            final Field verifierField = HCaptcha.class.getDeclaredField("captchaVerifier");
            verifierField.setAccessible(true);
            final Object verifier = verifierField.get(hCaptcha);
            final Field helperField = verifier.getClass().getDeclaredField("webViewHelper");
            helperField.setAccessible(true);
            return ((HCaptchaWebViewHelper) helperField.get(verifier)).getWebView();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
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
