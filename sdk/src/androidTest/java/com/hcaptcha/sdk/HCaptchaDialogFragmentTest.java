package com.hcaptcha.sdk;

import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.espresso.web.webdriver.DriverAtoms;
import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.clearElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.webClick;
import static com.hcaptcha.sdk.AssertUtil.waitToBeDisplayed;
import static com.hcaptcha.sdk.AssertUtil.waitToDisappear;
import static com.hcaptcha.sdk.HCaptchaDialogFragment.KEY_CONFIG;
import static com.hcaptcha.sdk.HCaptchaDialogFragment.KEY_LISTENER;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


@RunWith(AndroidJUnit4.class)
public class HCaptchaDialogFragmentTest {
    public class HCaptchaDialogTestAdapter extends HCaptchaDialogListener {
        @Override
        void onOpen() {
        }

        @Override
        void onSuccess(HCaptchaTokenResponse hCaptchaTokenResponse) {
        }

        @Override
        void onFailure(HCaptchaException hCaptchaException) {
        }
    };

    public FragmentScenario<HCaptchaDialogFragment> launchCaptchaFragment() {
        return launchCaptchaFragment(true);
    }

    public FragmentScenario<HCaptchaDialogFragment> launchCaptchaFragment(boolean showLoader) {
        return launchCaptchaFragment(showLoader, new HCaptchaDialogTestAdapter());
    }

    public FragmentScenario<HCaptchaDialogFragment> launchCaptchaFragment(HCaptchaDialogListener listener) {
        return launchCaptchaFragment(true, listener);
    }

    public FragmentScenario<HCaptchaDialogFragment> launchCaptchaFragment(boolean showLoader, HCaptchaDialogListener listener) {
        final HCaptchaConfig hCaptchaConfig = HCaptchaConfig.builder()
                .siteKey("10000000-ffff-ffff-ffff-000000000001")
                .endpoint("https://js.hcaptcha.com/1/api.js")
                .locale("en")
                .loading(showLoader)
                .size(HCaptchaSize.INVISIBLE)
                .theme(HCaptchaTheme.LIGHT)
                .build();
        final Bundle args = new Bundle();
        args.putSerializable(KEY_CONFIG, hCaptchaConfig);
        args.putParcelable(KEY_LISTENER, listener);
        return FragmentScenario.launchInContainer(HCaptchaDialogFragment.class, args);
    }

    @Test
    public void loaderVisible() {
        launchCaptchaFragment();
        onView(withId(R.id.loadingContainer)).check(matches(isDisplayed()));
        onView(withId(R.id.webView)).perform(waitToBeDisplayed(1000));
        onView(withId(R.id.loadingContainer)).perform(waitToDisappear(10000));
    }

    @Test
    public void loaderDisabled() {
        launchCaptchaFragment(false);
        onView(withId(R.id.loadingContainer)).check(matches(not(isDisplayed())));
        onView(withId(R.id.webView)).perform(waitToBeDisplayed(1000));
    }

    @Test
    public void webViewReturnToken() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final HCaptchaDialogListener listener = new HCaptchaDialogTestAdapter() {
            @Override
            void onSuccess(HCaptchaTokenResponse hCaptchaTokenResponse) {
                assertEquals("test-token", hCaptchaTokenResponse.getTokenResult());
                latch.countDown();
            }
        };

        final FragmentScenario<HCaptchaDialogFragment> scenario = launchCaptchaFragment(listener);
        onView(withId(R.id.webView)).perform(waitToBeDisplayed(1000));

        onWebView(withId(R.id.webView)).forceJavascriptEnabled();

        onWebView().withElement(findElement(Locator.ID, "input-text"))
                .perform(clearElement())
                .perform(DriverAtoms.webKeys("test-token"));

        onWebView().withElement(findElement(Locator.ID, "on-pass"))
                .perform(webClick());

        assertTrue(latch.await(1000, TimeUnit.MILLISECONDS)); // wait for callback
    }

    @Test
    public void webViewReturnsError() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final HCaptchaDialogListener listener = new HCaptchaDialogTestAdapter() {
            @Override
            void onFailure(HCaptchaException hCaptchaException) {
                assertEquals(HCaptchaError.SESSION_TIMEOUT, hCaptchaException.getHCaptchaError());
                latch.countDown();
            }
        };

        final FragmentScenario<HCaptchaDialogFragment> scenario = launchCaptchaFragment(listener);
        onView(withId(R.id.webView)).perform(waitToBeDisplayed(1000));

        onWebView(withId(R.id.webView)).forceJavascriptEnabled();

        onWebView().withElement(findElement(Locator.ID, "input-text"))
                .perform(clearElement())
                .perform(DriverAtoms.webKeys(
                        String.valueOf(HCaptchaError.SESSION_TIMEOUT.getErrorId())));

        onWebView().withElement(findElement(Locator.ID, "on-error"))
                .perform(webClick());

        assertTrue(latch.await(1000, TimeUnit.MILLISECONDS)); // wait for callback
    }

    @Test
    public void onOpenCallbackWorks() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final HCaptchaDialogListener listener = new HCaptchaDialogTestAdapter() {
            @Override
            void onOpen() {
                latch.countDown();
            }
        };

        final FragmentScenario<HCaptchaDialogFragment> scenario = launchCaptchaFragment(listener);
        onView(withId(R.id.webView)).perform(waitToBeDisplayed(1000));

        onWebView(withId(R.id.webView)).forceJavascriptEnabled();

        onWebView().withElement(findElement(Locator.ID, "input-text"))
                .perform(clearElement())
                .perform(DriverAtoms.webKeys("test-token"));

        onWebView().withElement(findElement(Locator.ID, "on-pass"))
                .perform(webClick());

        assertTrue(latch.await(1000, TimeUnit.MILLISECONDS)); // wait for callback
    }
}
