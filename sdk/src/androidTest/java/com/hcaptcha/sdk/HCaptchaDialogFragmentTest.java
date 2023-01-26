package com.hcaptcha.sdk;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.clearElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.webClick;
import static com.hcaptcha.sdk.AssertUtil.failAsNonReachable;
import static com.hcaptcha.sdk.AssertUtil.waitHCaptchaWebViewErrorByInput;
import static com.hcaptcha.sdk.AssertUtil.waitHCaptchaWebViewToken;
import static com.hcaptcha.sdk.AssertUtil.waitToBeDisplayed;
import static com.hcaptcha.sdk.AssertUtil.waitToDisappear;
import static com.hcaptcha.sdk.HCaptchaDialogFragment.KEY_CONFIG;
import static com.hcaptcha.sdk.HCaptchaDialogFragment.KEY_INTERNAL_CONFIG;
import static com.hcaptcha.sdk.HCaptchaDialogFragment.KEY_LISTENER;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.os.Bundle;
import android.util.AndroidRuntimeException;
import android.view.LayoutInflater;
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

    final HCaptchaInternalConfig internalConfig = HCaptchaInternalConfig.builder()
            .htmlProvider(new HCaptchaTestHtml())
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
        args.putSerializable(KEY_INTERNAL_CONFIG, internalConfig);
        args.putParcelable(KEY_LISTENER, listener);
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

        waitHCaptchaWebViewErrorByInput(latch, HCaptchaError.CHALLENGE_ERROR, AWAIT_CALLBACK_MS);
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

    @Test
    public void testClassCastExceptionHandled() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        final Bundle bundle = new Bundle();
        bundle.putSerializable(KEY_CONFIG, config);
        bundle.putSerializable(KEY_INTERNAL_CONFIG, "invalid config class");
        bundle.putParcelable(KEY_LISTENER, new HCaptchaStateTestAdapter() {
            @Override
            void onFailure(HCaptchaException exception) {
                assertEquals(HCaptchaError.ERROR, exception.getHCaptchaError());
                latch.countDown();
            }
        });

        FragmentScenario.launchInContainer(HCaptchaDialogFragment.class, bundle);

        assertTrue(latch.await(AWAIT_CALLBACK_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void webViewNotInstalled() throws InterruptedException {
        final LayoutInflater inflater = mock(LayoutInflater.class);
        when(inflater.inflate(eq(R.layout.hcaptcha_fragment), any(), eq(false)))
                .thenThrow(AndroidRuntimeException.class);

        final CountDownLatch latch = new CountDownLatch(1);

        final Bundle bundle = new Bundle();
        bundle.putSerializable(KEY_CONFIG, config);
        bundle.putSerializable(KEY_INTERNAL_CONFIG, internalConfig);
        bundle.putParcelable(KEY_LISTENER, new HCaptchaStateTestAdapter() {
            @Override
            void onFailure(HCaptchaException exception) {
                assertEquals(HCaptchaError.ERROR, exception.getHCaptchaError());
                latch.countDown();
            }
        });

        try (FragmentScenario<HCaptchaDialogFragment> scenario = FragmentScenario
                .launchInContainer(HCaptchaDialogFragment.class, bundle)) {
            scenario.onFragment(fragment -> {
                fragment.onCreateView(inflater, null, bundle);
            });
        }

        assertTrue(latch.await(AWAIT_CALLBACK_MS, TimeUnit.MILLISECONDS));
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

        launchCaptchaFragment(updatedWithRetry, listener);

        waitHCaptchaWebViewErrorByInput(failureLatch, HCaptchaError.NETWORK_ERROR, AWAIT_CALLBACK_MS);

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

        launchCaptchaFragment(updatedWithRetry, listener);

        waitHCaptchaWebViewErrorByInput(retryLatch, HCaptchaError.NETWORK_ERROR, AWAIT_CALLBACK_MS);

        assertTrue(failureLatch.await(AWAIT_CALLBACK_MS, TimeUnit.MILLISECONDS));
    }
}
