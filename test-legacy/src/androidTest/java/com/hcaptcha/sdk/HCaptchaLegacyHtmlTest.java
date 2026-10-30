package com.hcaptcha.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.hcaptcha.sdk.legacy.LegacyTestActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Checks that the bundled hcaptcha.html runs on the oldest supported WebViews (Android 4.1+).
 * <p>
 * `evaluateJavascript` and espresso-web are not available on API 16, so probes are injected
 * with `javascript:` URLs and report back through the `JSInterface` bridge.
 */
@RunWith(AndroidJUnit4.class)
public class HCaptchaLegacyHtmlTest {
    private static final long TIMEOUT_SECONDS = 10;

    private final CountDownLatch pageFinished = new CountDownLatch(1);
    private final List<String> uncaughtErrors = new CopyOnWriteArrayList<>();
    private final TestBridge bridge = new TestBridge();

    private ActivityScenario<LegacyTestActivity> activityScenario;
    private WebView webView;

    @Before
    public void setUp() throws InterruptedException {
        activityScenario = ActivityScenario.launch(LegacyTestActivity.class);
        activityScenario.onActivity(activity -> {
            webView = new HCaptchaWebView(activity);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.addJavascriptInterface(bridge, "JSInterface");
            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                    final String message = consoleMessage.message();
                    if (message != null && message.startsWith("Uncaught")) {
                        uncaughtErrors.add(message + " (line " + consoleMessage.lineNumber() + ")");
                    }
                    return super.onConsoleMessage(consoleMessage);
                }
            });
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    pageFinished.countDown();
                }
            });
            activity.setContentView(webView);
            webView.loadDataWithBaseURL(null, new HCaptchaHtml().getHtml(), "text/html", "UTF-8", null);
        });
        assertTrue("Page should finish loading", pageFinished.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }

    @After
    public void tearDown() {
        if (activityScenario != null) {
            activityScenario.onActivity(activity -> webView.destroy());
            activityScenario.close();
        }
    }

    @Test
    public void testLoadApiRunsOnLegacyWebView() throws InterruptedException {
        final String result = probe(
                "JSInterface.report(String(!!document.head.querySelector('script[src^=\"null?\"]')));");

        assertEquals("No uncaught JS errors expected", "[]", uncaughtErrors.toString());
        assertEquals("loadApi() should append the api.js script", "true", result);
    }

    // Checks the effect, not just that nothing throws: API 16-19 WebViews ignore unprefixed `transform`.
    @Test
    public void testFitChallengeToViewportScalesTallOverlay() throws InterruptedException {
        final String result = probe("try {"
                + "  var overlay = document.createElement('div');"
                + "  overlay.style.position = 'absolute';"
                + "  overlay.style.top = '-100px';"
                + "  overlay.style.left = '0';"
                + "  var frame = document.createElement('iframe');"
                + "  frame.style.display = 'block';"
                + "  frame.style.border = '0';"
                + "  frame.style.height = Math.round(window.innerHeight * 1.5) + 'px';"
                + "  overlay.appendChild(frame);"
                + "  document.body.appendChild(overlay);"
                + "  fitChallengeToViewport();"
                + "  var rect = overlay.getBoundingClientRect();"
                + "  JSInterface.report(rect.top >= 0 && rect.bottom <= window.innerHeight + 1"
                + "      ? 'fits' : 'clipped: ' + rect.top + '..' + rect.bottom + ' / ' + window.innerHeight);"
                + "} catch (e) {"
                + "  JSInterface.report('error: ' + e);"
                + "}");

        assertEquals("fitChallengeToViewport() should bring the overlay on screen", "fits", result);
        assertEquals("No uncaught JS errors expected", "[]", uncaughtErrors.toString());
    }

    private String probe(String script) throws InterruptedException {
        activityScenario.onActivity(activity -> webView.loadUrl("javascript:" + script));
        assertTrue("Probe should report back", bridge.awaitReport());
        return bridge.getReport();
    }

    /**
     * Stub for the `JSInterface` bridge used by hcaptcha.html, plus a `report` hook for probes.
     */
    public static class TestBridge {
        private final CountDownLatch reportLatch = new CountDownLatch(1);
        private final AtomicReference<String> report = new AtomicReference<>();

        @JavascriptInterface
        public String getConfig() {
            // jsSrc is null so the real hCaptcha api.js is never loaded
            return "{\"siteKey\":\"10000000-ffff-ffff-ffff-000000000001\",\"locale\":\"en\","
                    + "\"size\":\"invisible\",\"jsSrc\":null}";
        }

        @JavascriptInterface
        public void onLoaded() {
        }

        @JavascriptInterface
        public void onOpen() {
        }

        @JavascriptInterface
        public void onPass(String token) {
        }

        @JavascriptInterface
        public void onError(int errorCode) {
        }

        @JavascriptInterface
        public void report(String value) {
            report.set(value);
            reportLatch.countDown();
        }

        boolean awaitReport() throws InterruptedException {
            return reportLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        String getReport() {
            return report.get();
        }
    }
}
