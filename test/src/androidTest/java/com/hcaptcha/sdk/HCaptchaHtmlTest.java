package com.hcaptcha.sdk;

import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.test.core.app.ActivityScenario;

import static androidx.test.espresso.web.model.Atoms.castOrDie;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static androidx.test.espresso.web.model.Atoms.script;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.hcaptcha.sdk.test.TestActivity;

@RunWith(AndroidJUnit4.class)
public class HCaptchaHtmlTest {

    private ActivityScenario<TestActivity> activityScenario;
    private WebView webView;
    private TestJSInterface testJSInterface;
    private MockHcaptcha mockHCaptcha;

    @Before
    public void setUp() {
        testJSInterface = new TestJSInterface();
        mockHCaptcha = new MockHcaptcha();
        activityScenario = ActivityScenario.launch(TestActivity.class);
    }

    @After
    public void tearDown() {
        if (activityScenario != null) {
            activityScenario.onActivity(activity -> webView.destroy());
            activityScenario.close();
        }
    }

    private void setupWebView(Runnable onWebViewReady) {
        activityScenario.onActivity(activity -> {
            webView = new HCaptchaWebView(activity);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.addJavascriptInterface(testJSInterface, "JSInterface");
            webView.addJavascriptInterface(mockHCaptcha, "hcaptcha");

            activity.setContentView(webView);

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    if (onWebViewReady != null) {
                        onWebViewReady.run();
                    }
                }
            });

            webView.loadDataWithBaseURL(null,
                new com.hcaptcha.sdk.HCaptchaHtml().getHtml(), "text/html", "UTF-8", null);
        });
    }

    @Test
    public void testLoadApiAddsScriptNodeWithUndefinedSrc() {
        setupWebView(null);

        onView(withId(android.R.id.content))
                .check(matches(isDisplayed()));

        onWebView()
                .forceJavascriptEnabled()
                .check(webMatches(
                        script(
                                "return !!document.head.querySelector('script[src^=\"null?\"]');",
                                castOrDie(Boolean.class)
                        ),
                        is(true)
                ));
    }

    @Test
    public void testResetFunctionIsCallable() throws InterruptedException {
        setupWebView(() -> {
            webView.evaluateJavascript("reset()", null);
        });

        assertTrue("Mock hcaptcha.reset() should be called", mockHCaptcha.isResetCalled());
    }

    @Test
    public void testOnHcaptchaLoadedFunctionIsCallable() throws InterruptedException {
        setupWebView(() -> {
            webView.evaluateJavascript("onHcaptchaLoaded()", null);
        });

        assertTrue("Mock onLoaded should be called", testJSInterface.isOnLoadedCalled());
    }

    @Test
    public void testResetAndExecuteWithJsonParams() throws InterruptedException {
        final String verifyParams = "{\"mfa_phoneprefix\": \"1\"}";
        setupWebView(() -> {
            webView.evaluateJavascript("reset()", null);
            webView.evaluateJavascript("setData(" + verifyParams + ")", null);
            webView.evaluateJavascript("execute()", null);
        });

        // Check if mock hcaptcha.setData() was called with the JSON parameters
        assertTrue("Mock hcaptcha.setData() should be called", mockHCaptcha.isSetDataCalled());

        // Note: android does not support JS object serialization.
        String setDataParams = mockHCaptcha.getLastSetDataParams();
        assertEquals("setData should contain expected JSON data",
                "undefined", setDataParams);
    }

    @Test
    public void testRenderFunctionIsCalledWithCorrectContainerId() throws InterruptedException {
        setupWebView(() -> {
            // Test that render function is called with correct container ID
            webView.evaluateJavascript("onHcaptchaLoaded()", null);
        });

        assertTrue("Mock hcaptcha.render() should be called", mockHCaptcha.isRenderCalled());

        String renderParams = mockHCaptcha.getLastRenderId();
        assertEquals("render should be called with correct container ID",
                "hcaptcha-container", renderParams);
    }

    /**
     * Stub implementation of HCaptchaJSInterface for testing
     */
    public static class TestJSInterface {
        private final CountDownLatch onLoadedLatch = new CountDownLatch(1);

        @JavascriptInterface
        public String getConfig() {
            // Note: jsSrc is intentionally null/undefined to prevent real hCaptcha loading
            // This allows us to use our mock hcaptcha implementation for testing
            return "{\"siteKey\":\"10000000-ffff-ffff-ffff-000000000001\",\"locale\":\"en\",\"size\":\"compact\",\"jsSrc\":null}";
        }

        @JavascriptInterface
        public void onLoaded() {
            onLoadedLatch.countDown();
        }
        public boolean isOnLoadedCalled() throws InterruptedException {
            return onLoadedLatch.await(5, TimeUnit.SECONDS);
        }
    }

    /**
     * Mock implementation of hcaptcha object for testing
     */
    public static class MockHcaptcha {
        private final CountDownLatch setDataLatch = new CountDownLatch(1);
        private final CountDownLatch executeLatch = new CountDownLatch(1);
        private final CountDownLatch resetLatch = new CountDownLatch(1);
        private final CountDownLatch closeLatch = new CountDownLatch(1);
        private final CountDownLatch renderLatch = new CountDownLatch(1);

        private String lastSetDataParams = null;
        private String lastExecuteParams = null;
        private String lastRenderParams = null;

        @JavascriptInterface
        public void setData(String hCaptchaID, String data) {
            this.lastSetDataParams = data;
            setDataLatch.countDown();
        }

        @JavascriptInterface
        public void execute(String hCaptchaID) {
            this.lastExecuteParams = "hCaptchaID: " + hCaptchaID;
            executeLatch.countDown();
        }

        @JavascriptInterface
        public void reset() {
            resetLatch.countDown();
        }

        @JavascriptInterface
        public void close() {
            closeLatch.countDown();
        }

        @JavascriptInterface
        public String render(String containerId, String config) {
            this.lastRenderParams = containerId;
            renderLatch.countDown();
            return "mock-hcaptcha-id-123";
        }

        // Getters for testing - wait for specific function calls
        public boolean isSetDataCalled() throws InterruptedException {
            return setDataLatch.await(5, TimeUnit.SECONDS);
        }
        public boolean isExecuteCalled() throws InterruptedException {
            return executeLatch.await(5, TimeUnit.SECONDS);
        }
        public boolean isResetCalled() throws InterruptedException {
            return resetLatch.await(5, TimeUnit.SECONDS);
        }
        public boolean isCloseCalled() throws InterruptedException {
            return closeLatch.await(5, TimeUnit.SECONDS);
        }
        public boolean isRenderCalled() throws InterruptedException {
            return renderLatch.await(5, TimeUnit.SECONDS);
        }

        // Parameter getters
        public String getLastSetDataParams() { return lastSetDataParams; }
        public String getLastExecuteParams() { return lastExecuteParams; }
        public String getLastRenderId() { return lastRenderParams; }
    }
}

