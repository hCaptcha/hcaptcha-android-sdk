package com.hcaptcha.sdk;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.clearElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.webClick;
import static com.hcaptcha.sdk.AssertUtil.waitToBeDisplayed;
import static com.hcaptcha.sdk.AssertUtil.waitToDisappear;
import static com.hcaptcha.sdk.HCaptchaDialogFragment.KEY_CONFIG;
import static com.hcaptcha.sdk.HCaptchaDialogFragment.KEY_HTML;
import static com.hcaptcha.sdk.HCaptchaDialogFragment.KEY_LISTENER;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.espresso.web.webdriver.DriverAtoms;
import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


@RunWith(AndroidJUnit4.class)
public class HCaptchaDialogFragmentTest {
    private static final long AWAIT_CALLBACK_MS = 1000;
    private static final String TEST_TOKEN = "test-token";

    final HCaptchaConfig config = HCaptchaConfig.builder()
            .siteKey("10000000-ffff-ffff-ffff-000000000001")
            .endpoint("https://js.hcaptcha.com/1/api.js")
            .locale("en")
            .loading(true)
            .size(HCaptchaSize.INVISIBLE)
            .theme(HCaptchaTheme.LIGHT)
            .build();

    private FragmentScenario<HCaptchaDialogFragment> launchCaptchaFragment() {
        return launchCaptchaFragment(true);
    }

    private FragmentScenario<HCaptchaDialogFragment> launchCaptchaFragment(boolean showLoader) {
        return launchCaptchaFragment(config.toBuilder().loading(showLoader).build(), new HCaptchaStateTestAdapter());
    }

    private FragmentScenario<HCaptchaDialogFragment> launchCaptchaFragment(HCaptchaStateListener listener) {
        return launchCaptchaFragment(config, listener);
    }

    private FragmentScenario<HCaptchaDialogFragment> launchCaptchaFragment(final HCaptchaConfig captchaConfig,
                                                                           HCaptchaStateListener listener) {
        final Bundle args = new Bundle();
        args.putSerializable(KEY_CONFIG, captchaConfig);
        args.putParcelable(KEY_LISTENER, listener);
        args.putSerializable(KEY_HTML, new HCaptchaTestHtml());
        return FragmentScenario.launchInContainer(HCaptchaDialogFragment.class, args);
    }

    private void waitForWebViewToEmitToken(final CountDownLatch latch)
            throws InterruptedException {
        onView(withId(R.id.webView)).perform(waitToBeDisplayed());

        onWebView(withId(R.id.webView)).forceJavascriptEnabled();

        onWebView().withElement(findElement(Locator.ID, "input-text"))
                .perform(clearElement())
                .perform(DriverAtoms.webKeys(TEST_TOKEN));

        onWebView().withElement(findElement(Locator.ID, "on-pass"))
                .perform(webClick());

        assertTrue(latch.await(AWAIT_CALLBACK_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void loaderVisible() {
        launchCaptchaFragment();
        onView(withId(R.id.loadingContainer)).check(matches(isDisplayed()));
        onView(withId(R.id.webView)).perform(waitToBeDisplayed());
        final long waitToDisappearMs = 10000;
        onView(withId(R.id.loadingContainer)).perform(waitToDisappear(waitToDisappearMs));
    }

    @Test
    public void loaderDisabled() {
        launchCaptchaFragment(false);
        onView(withId(R.id.loadingContainer)).check(matches(not(isDisplayed())));
        onView(withId(R.id.webView)).perform(waitToBeDisplayed());
    }

    @Test
    public void webViewReturnToken() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final HCaptchaStateListener listener = new HCaptchaStateTestAdapter() {
            @Override
            void onSuccess(String token) {
                assertEquals(TEST_TOKEN, token);
                latch.countDown();
            }
        };

        launchCaptchaFragment(listener);
        waitForWebViewToEmitToken(latch);
    }

    @Test
    public void webViewReturnsError() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final HCaptchaStateListener listener = new HCaptchaStateTestAdapter() {
            @Override
            void onFailure(HCaptchaException exception) {
                assertEquals(HCaptchaError.CHALLENGE_ERROR, exception.getHCaptchaError());
                latch.countDown();
            }
        };

        launchCaptchaFragment(listener);
        onView(withId(R.id.webView)).perform(waitToBeDisplayed());

        onWebView(withId(R.id.webView)).forceJavascriptEnabled();

        onWebView().withElement(findElement(Locator.ID, "input-text"))
                .perform(clearElement())
                .perform(DriverAtoms.webKeys(
                        String.valueOf(HCaptchaError.CHALLENGE_ERROR.getErrorId())));

        onWebView().withElement(findElement(Locator.ID, "on-error"))
                .perform(webClick());

        assertTrue(latch.await(AWAIT_CALLBACK_MS, TimeUnit.MILLISECONDS)); // wait for callback
    }

    @Test
    public void onOpenCallbackWorks() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final HCaptchaStateListener listener = new HCaptchaStateTestAdapter() {
            @Override
            void onOpen() {
                latch.countDown();
            }
        };

        launchCaptchaFragment(listener);
        waitForWebViewToEmitToken(latch);
    }
}
