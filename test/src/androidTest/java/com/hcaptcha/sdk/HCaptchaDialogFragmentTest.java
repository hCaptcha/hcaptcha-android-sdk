package com.hcaptcha.sdk;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.clearElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.webClick;
import static com.hcaptcha.sdk.AssertUtil.waitHCaptchaWebViewErrorByInput;
import static com.hcaptcha.sdk.AssertUtil.waitHCaptchaWebViewToken;
import static com.hcaptcha.sdk.AssertUtil.waitToBeDisplayed;
import static com.hcaptcha.sdk.AssertUtil.waitToDisappear;
import static com.hcaptcha.sdk.HCaptchaDialogFragment.KEY_CONFIG;
import static com.hcaptcha.sdk.HCaptchaDialogFragment.KEY_INTERNAL_CONFIG;
import static com.hcaptcha.sdk.HCaptchaDialogFragment.KEY_LISTENER;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Dialog;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.InflateException;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.web.webdriver.DriverAtoms;
import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.hcaptcha.sdk.test.TestActivity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;

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

    private FragmentScenario<HCaptchaDialogFragment> launchInContainer() {
        return launchInContainer(true);
    }

    private FragmentScenario<HCaptchaDialogFragment> launchInContainer(boolean showLoader) {
        return launchInContainer(config.toBuilder().loading(showLoader).build(), new HCaptchaStateTestAdapter());
    }

    private FragmentScenario<HCaptchaDialogFragment> launchInContainer(HCaptchaStateListener listener) {
        return launchInContainer(config, listener);
    }

    private FragmentScenario<HCaptchaDialogFragment> launchInContainer(final HCaptchaConfig captchaConfig,
                                                                       final HCaptchaStateListener listener) {
        return launchInContainer(captchaConfig, internalConfig, listener);
    }

    private FragmentScenario<HCaptchaDialogFragment> launchInContainer(
            final HCaptchaConfig captchaConfig,
            final HCaptchaInternalConfig internalCaptchaConfig,
            final HCaptchaStateListener listener) {
        return launchInContainer(captchaConfig, internalCaptchaConfig, listener, Lifecycle.State.RESUMED);
    }

    private FragmentScenario<HCaptchaDialogFragment> launchInContainer(
            final HCaptchaConfig captchaConfig,
            final HCaptchaInternalConfig internalCaptchaConfig,
            final HCaptchaStateListener listener,
            final Lifecycle.State initialState) {
        final Bundle args = new Bundle();
        args.putSerializable(KEY_CONFIG, captchaConfig);
        args.putSerializable(KEY_INTERNAL_CONFIG, internalCaptchaConfig);
        args.putParcelable(KEY_LISTENER, listener);
        return FragmentScenario.launchInContainer(HCaptchaDialogFragment.class,
                args, R.style.HCaptchaDialogTheme, initialState);
    }

    private FragmentScenario<HCaptchaDialogFragment> launch(
            final HCaptchaConfig captchaConfig,
            final HCaptchaInternalConfig internalCaptchaConfig,
            final HCaptchaStateListener listener) {
        final Bundle args = new Bundle();
        args.putSerializable(KEY_CONFIG, captchaConfig);
        args.putSerializable(KEY_INTERNAL_CONFIG, internalCaptchaConfig);
        args.putParcelable(KEY_LISTENER, listener);
        return FragmentScenario.launch(HCaptchaDialogFragment.class, args);
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
        launchInContainer();
        onView(withId(R.id.loadingContainer)).check(matches(isDisplayed()));
        onView(withId(R.id.webView)).perform(waitToBeDisplayed());
        final long waitToDisappearMs = 10000;
        onView(withId(R.id.loadingContainer)).perform(waitToDisappear(waitToDisappearMs));
    }

    @Test
    public void loaderDisabled() {
        launchInContainer(false);
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

        launchInContainer(listener);
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

        launchInContainer(listener);

        waitHCaptchaWebViewErrorByInput(latch, HCaptchaError.CHALLENGE_ERROR, AWAIT_CALLBACK_MS);
    }

    @Test
    public void onOpenCallbackWorks() throws Exception {
        final CountDownLatch latch = new CountDownLatch(2);
        final HCaptchaStateListener listener = new HCaptchaStateTestAdapter() {
            @Override
            void onOpen() {
                latch.countDown();
            }

            @Override
            void onSuccess(String response) {
                latch.countDown();
            }
        };

        launchInContainer(listener);
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
        when(inflater.inflate(ArgumentMatchers.eq(R.layout.hcaptcha_fragment), any(), eq(false)))
                .thenThrow(InflateException.class);

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
        };

        final HCaptchaConfig updatedWithRetry = config.toBuilder()
                .retryPredicate((c, e) -> {
                    failureLatch.countDown();
                    return e.getHCaptchaError() == HCaptchaError.NETWORK_ERROR;
                })
                .build();

        launchInContainer(updatedWithRetry, listener);

        waitHCaptchaWebViewErrorByInput(failureLatch, HCaptchaError.NETWORK_ERROR, AWAIT_CALLBACK_MS);

        waitHCaptchaWebViewToken(successLatch, AWAIT_CALLBACK_MS);
    }

    @Test
    public void testNotRetryPredicate() throws Exception {
        final CountDownLatch failureLatch = new CountDownLatch(1);
        final CountDownLatch retryLatch = new CountDownLatch(1);
        final HCaptchaStateListener listener = new HCaptchaStateTestAdapter() {

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

        launchInContainer(updatedWithRetry, listener);

        waitHCaptchaWebViewErrorByInput(retryLatch, HCaptchaError.NETWORK_ERROR, AWAIT_CALLBACK_MS);

        assertTrue(failureLatch.await(AWAIT_CALLBACK_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testPauseResumeFragment() throws Exception {
        final CountDownLatch successLatch = new CountDownLatch(1);
        final HCaptchaStateListener listener = new HCaptchaStateTestAdapter() {

            @Override
            void onSuccess(String token) {
                successLatch.countDown();
            }
        };

        final FragmentScenario<HCaptchaDialogFragment> scenario = launchInContainer(config, listener);
        scenario.moveToState(Lifecycle.State.STARTED).moveToState(Lifecycle.State.RESUMED);

        waitHCaptchaWebViewToken(successLatch, AWAIT_CALLBACK_MS);

        assertTrue(successLatch.await(AWAIT_CALLBACK_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testActivityRecreate() throws Exception {
        final CountDownLatch successLatch = new CountDownLatch(1);
        final HCaptchaStateListener listener = new HCaptchaStateTestAdapter() {

            @Override
            void onSuccess(String token) {
                successLatch.countDown();
            }
        };

        final FragmentScenario<HCaptchaDialogFragment> scenario = launchInContainer(config, listener);
        scenario.recreate();

        waitHCaptchaWebViewToken(successLatch, AWAIT_CALLBACK_MS);

        assertTrue(successLatch.await(AWAIT_CALLBACK_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testVerifyOnStoppedFragmentNoException() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        try (ActivityScenario<TestActivity> scenario = ActivityScenario.launch(TestActivity.class)) {
            scenario.moveToState(Lifecycle.State.CREATED).onActivity(activity -> {
                HCaptchaDialogFragment.newInstance(config, internalConfig,
                                new HCaptchaStateTestAdapter())
                        .startVerification(activity);
                latch.countDown();
            });
        }
        assertTrue(latch.await(AWAIT_CALLBACK_MS, TimeUnit.MILLISECONDS));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReset() {
        final FragmentScenario<HCaptchaDialogFragment> scenario = launchInContainer(
                config, new HCaptchaStateTestAdapter());

        scenario.onFragment(HCaptchaDialogFragment::reset);

        // The fragment has been removed from the FragmentManager already.
        scenario.onFragment(fragment -> assertTrue(fragment.isDetached()));
    }

    @Test
    public void testBackShouldCloseCaptcha() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final HCaptchaStateListener listener = new HCaptchaStateTestAdapter() {
            @Override
            void onFailure(HCaptchaException exception) {
                assertEquals(HCaptchaError.CHALLENGE_CLOSED, exception.getHCaptchaError());
                latch.countDown();
            }
        };

        try (FragmentScenario<HCaptchaDialogFragment> scenario = launch(config, internalConfig, listener)) {
            scenario.onFragment(fragment -> {
                final Dialog dialog = fragment.getDialog();
                assertNotNull(dialog);
                assertTrue(dialog.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK)));
                assertTrue(dialog.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK)));
            });
        }

        assertTrue(latch.await(AWAIT_CALLBACK_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testBackShouldNotCloseCaptchaWithoutDefaultLoadingIndicator() {
        try (FragmentScenario<HCaptchaDialogFragment> scenario = launch(
                config.toBuilder()
                        .loading(false)
                        .build(),
                internalConfig.toBuilder()
                        .htmlProvider(new HCaptchaTestHtml(false))
                        .build(),
                new HCaptchaStateTestAdapter())) {

            scenario.onFragment(fragment -> {
                final Dialog dialog = fragment.getDialog();
                assertNotNull(dialog);
                final KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK);
                assertTrue(dialog.dispatchKeyEvent(keyEvent));
            });
        }
    }

    @Test
    public void testTouchShouldNotCloseCaptchaWithoutDefaultLoadingIndicator() {
        try (FragmentScenario<HCaptchaDialogFragment> scenario = launch(
                config.toBuilder()
                        .loading(false)
                        .build(),
                internalConfig.toBuilder()
                        .htmlProvider(new HCaptchaTestHtml(false))
                        .build(),
                new HCaptchaStateTestAdapter())) {

            scenario.onFragment(fragment -> {
                final Dialog dialog = fragment.getDialog();
                assertNotNull(dialog);
                final DisplayMetrics dm = new DisplayMetrics();
                final MotionEvent keyEvent = MotionEvent.obtain(
                        SystemClock.uptimeMillis() - 1,
                        SystemClock.uptimeMillis(),
                        MotionEvent.ACTION_DOWN,
                        dm.widthPixels / 2f,
                        dm.heightPixels / 2f,
                        0 /* NO_META */);
                assertTrue(dialog.dispatchTouchEvent(keyEvent));
            });
        }
    }
}
