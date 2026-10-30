package com.hcaptcha.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.os.Build;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;

import com.hcaptcha.sdk.test.TestActivity;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Crash a real renderer while using the real HCaptcha client and verifier.
 * Local HTML keeps recovery independent of the network and the hCaptcha service.
 */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
public class HCaptchaRenderProcessRecoveryTest {
    private static final long AWAIT_CALLBACK_MS = 15000;
    private static final String TOKEN = "recovered-renderer-token";
    private static final String LOCAL_HTML = "<html><body><script>"
            + "function reset() {}"
            + "function execute() { JSInterface.onPass('" + TOKEN + "'); }"
            + "window.onload = function() { JSInterface.onLoaded(); };"
            + "</script></body></html>";

    private final CountDownLatch loadedLatch = new CountDownLatch(1);
    private final CountDownLatch outcomeLatch = new CountDownLatch(1);
    private final CountDownLatch successLatch = new CountDownLatch(1);
    private final AtomicInteger loadedCount = new AtomicInteger();
    private final AtomicReference<HCaptchaException> failure = new AtomicReference<>();
    private final AtomicReference<String> token = new AtomicReference<>();

    private ActivityScenario<TestActivity> scenario;
    private HCaptcha client;
    private HCaptchaWebView originalWebView;

    @After
    public void tearDown() {
        if (scenario != null) {
            try {
                scenario.onActivity(activity -> {
                    if (client != null) {
                        client.destroy();
                    }
                });
            } finally {
                scenario.close();
            }
        }
    }

    @Test
    public void nextVerificationAfterRendererCrashUsesFreshWebView() throws Exception {
        final HCaptchaConfig config = configBuilder().build();
        setupClient(config);
        crashRenderer();

        assertTrue("Renderer failure should be delivered",
                outcomeLatch.await(AWAIT_CALLBACK_MS, TimeUnit.MILLISECONDS));
        assertNotNull("Renderer termination should report a failure", failure.get());
        assertEquals(HCaptchaError.ERROR, failure.get().getHCaptchaError());

        scenario.onActivity(activity -> {
            assertNull("The dead WebView must be detached", originalWebView.getParent());
            client.verifyWithHCaptcha(config);
            final HCaptchaWebView replacement = activity.findViewById(R.id.webView);
            assertNotNull("The next verification must attach a new WebView", replacement);
            assertNotSame("The dead WebView must not be reused", originalWebView, replacement);
        });

        assertTrue("The new renderer should complete verification",
                successLatch.await(AWAIT_CALLBACK_MS, TimeUnit.MILLISECONDS));
        assertEquals(TOKEN, token.get());
        assertEquals("The replacement must load its HTML again", 2, loadedCount.get());
    }

    @Test
    public void retryPredicateMustNotSwallowRendererCrash() throws Exception {
        final HCaptchaConfig config = configBuilder()
                .retryPredicate((currentConfig, exception) -> exception.getHCaptchaError() == HCaptchaError.ERROR)
                .build();
        setupClient(config);
        crashRenderer();

        // A terminal failure or successful recovery on a fresh renderer is acceptable.
        // A silent JavaScript retry on the destroyed WebView produces neither.
        assertTrue("Renderer termination must report a failure or recover on a fresh WebView",
                outcomeLatch.await(AWAIT_CALLBACK_MS, TimeUnit.MILLISECONDS));
        scenario.onActivity(activity ->
                assertNull("The dead WebView must be detached", originalWebView.getParent()));
        if (failure.get() != null) {
            assertEquals(HCaptchaError.ERROR, failure.get().getHCaptchaError());
        } else {
            assertEquals(TOKEN, token.get());
            assertEquals("Recovery must load a fresh WebView", 2, loadedCount.get());
        }
    }

    private HCaptchaConfig.HCaptchaConfigBuilder configBuilder() {
        return HCaptchaConfig.builder()
                .siteKey("10000000-ffff-ffff-ffff-000000000001")
                .renderMode(HCaptchaRenderMode.HEADLESS)
                .size(HCaptchaSize.INVISIBLE)
                .loading(false);
    }

    private void setupClient(HCaptchaConfig config) {
        final HCaptchaInternalConfig internalConfig = HCaptchaInternalConfig.builder()
                .htmlProvider(() -> LOCAL_HTML)
                .build();
        scenario = ActivityScenario.launch(TestActivity.class);
        scenario.onActivity(activity -> {
            client = HCaptcha.getClient(activity, internalConfig);
            client.addOnLoadedListener(() -> {
                loadedCount.incrementAndGet();
                loadedLatch.countDown();
            });
            client.addOnFailureListener(exception -> {
                failure.set(exception);
                outcomeLatch.countDown();
            });
            client.addOnSuccessListener(response -> {
                token.set(response.getTokenResult());
                successLatch.countDown();
                outcomeLatch.countDown();
            });
            client.setup(config);
            originalWebView = activity.findViewById(R.id.webView);
            assertNotNull("The preloaded headless WebView must be attached", originalWebView);
        });
    }

    private void crashRenderer() throws InterruptedException {
        assertTrue("Local HTML must load before crashing the renderer",
                loadedLatch.await(AWAIT_CALLBACK_MS, TimeUnit.MILLISECONDS));
        // Wait for a renderer round-trip rather than guessing when the initial navigation
        // has settled. Do not replace the SDK's WebViewClient with a test implementation.
        scenario.onActivity(activity -> originalWebView.evaluateJavascript("true",
                value -> originalWebView.loadUrl("chrome://crash")));
    }
}
