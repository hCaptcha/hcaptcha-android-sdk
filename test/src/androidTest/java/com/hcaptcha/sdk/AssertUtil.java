package com.hcaptcha.sdk;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static androidx.test.espresso.web.model.Atoms.getCurrentUrl;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.clearElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.webClick;
import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.graphics.drawable.ColorDrawable;
import android.view.View;
import androidx.annotation.ColorInt;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.util.HumanReadables;
import androidx.test.espresso.util.TreeIterables;
import androidx.test.espresso.web.webdriver.DriverAtoms;
import androidx.test.espresso.web.webdriver.Locator;

import org.hamcrest.Matcher;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Helper class with different ViewActions
 */
public final class AssertUtil {

    private static final long MAIN_LOOP_DELAY_MS = 50;
    private static final long WAIT_TO_BE_DISPLAYED_MS = 1000;

    private AssertUtil() {
    }

    public static ViewAction waitToBeDisplayed() {
        return waitToBeDisplayed(WAIT_TO_BE_DISPLAYED_MS);
    }

    public static ViewAction waitToBeDisplayed(final long millis) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return any(View.class);
            }

            @Override
            public String getDescription() {
                return "wait for view displayed for " + millis + " ms";
            }

            @Override
            public void perform(final UiController uiController, final View view) {
                uiController.loopMainThreadUntilIdle();
                final long endTime = System.currentTimeMillis() + millis;
                final Matcher<View> viewIsDisplayed = isDisplayed();
                do {
                    for (View child : TreeIterables.breadthFirstViewTraversal(view)) {
                        if (viewIsDisplayed.matches(child)) {
                            return;
                        }
                    }
                    uiController.loopMainThreadForAtLeast(MAIN_LOOP_DELAY_MS);
                }
                while (System.currentTimeMillis() < endTime);
                throw new PerformException.Builder()
                        .withActionDescription(this.getDescription())
                        .withViewDescription(HumanReadables.describe(view))
                        .withCause(new TimeoutException())
                        .build();
            }
        };
    }

    public static ViewAction waitToDisappear(final long millis) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return any(View.class);
            }

            @Override
            public String getDescription() {
                return "wait for view to be gone for " + millis + " ms ";
            }

            @Override
            public void perform(final UiController uiController, final View view) {
                final long endTime = System.currentTimeMillis() + millis;

                do {
                    if (view.getVisibility() == View.GONE || view.getVisibility() == View.INVISIBLE) {
                        return;
                    }
                    uiController.loopMainThreadForAtLeast(MAIN_LOOP_DELAY_MS);
                } while (System.currentTimeMillis() < endTime);

                throw new PerformException.Builder()
                        .withActionDescription(this.getDescription())
                        .withViewDescription(HumanReadables.describe(view))
                        .withCause(new TimeoutException())
                        .build();
            }
        };
    }

    // https://stackoverflow.com/a/38385064/902217
    public abstract static class EvaluateJsAction implements ViewAction, ValueCallback<String> {}  // NOPMD

    public static ViewAction evaluateJavascript(final String jsCode) {
        return new EvaluateJsAction() {
            private static final long TIME_OUT = 5000;
            private final AtomicBoolean mEvaluateFinished = new AtomicBoolean(false);

            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(WebView.class);
            }

            @Override
            public String getDescription() {
                return "evaluate '" + jsCode + "' on webview";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();

                final WebView webView = (WebView) view;
                webView.evaluateJavascript(jsCode, this);

                final long timeOut = System.currentTimeMillis() + TIME_OUT;
                while (!mEvaluateFinished.get()) {
                    if (timeOut < System.currentTimeMillis()) {
                        throw new PerformException.Builder()
                                .withActionDescription(this.getDescription())
                                .withViewDescription(HumanReadables.describe(view))
                                .withCause(new RuntimeException(String.format(Locale.US,
                                        "Evaluating java script did not finish after %d ms of waiting.", TIME_OUT)))
                                .build();
                    }
                    uiController.loopMainThreadForAtLeast(MAIN_LOOP_DELAY_MS);
                }
            }

            @Override
            public void onReceiveValue(String value) {
                mEvaluateFinished.set(true);
            }
        };
    }

    public static void waitHCaptchaWebViewToken(final CountDownLatch latch, final long timeout)
            throws InterruptedException {
        onWebView().check(webMatches(getCurrentUrl(), startsWith("about:blank")));
        onView(withId(R.id.webView)).perform(evaluateJavascript("onPass(\"some-token\")"));
        assertTrue(latch.await(timeout, TimeUnit.MILLISECONDS));
    }

    public static void waitHCaptchaWebViewError(final CountDownLatch latch,
                                                final HCaptchaError error,
                                                final long timeout)
            throws InterruptedException {
        onWebView().check(webMatches(getCurrentUrl(), startsWith("about:blank")));
        onView(withId(R.id.webView)).perform(evaluateJavascript(
                "onError(" + error.getErrorId() + ")"));
        assertTrue(latch.await(timeout, TimeUnit.MILLISECONDS));
    }

    public static void waitHCaptchaWebViewErrorByInput(final CountDownLatch latch,
                                                       final HCaptchaError error,
                                                       final long timeout)
            throws InterruptedException {
        onView(withId(R.id.webView)).perform(waitToBeDisplayed());

        onWebView(withId(R.id.webView)).forceJavascriptEnabled();

        onWebView().withElement(findElement(Locator.ID, "input-text"))
                .perform(clearElement())
                .perform(DriverAtoms.webKeys(
                        String.valueOf(error.getErrorId())));

        onWebView().withElement(findElement(Locator.ID, "on-error"))
                .perform(webClick());

        assertTrue(latch.await(timeout, TimeUnit.MILLISECONDS)); // wait for callback
    }

    public static void failAsNonReachable() {
        fail("Should not be called for this test");
    }

    public static org.hamcrest.Matcher<View> withBackgroundColor(@ColorInt final int expectedColor) {
        return new org.hamcrest.TypeSafeMatcher<View>() {
            @Override
            public void describeTo(org.hamcrest.Description description) {
                description.appendText("with background color: ").appendValue(expectedColor);
            }

            @Override
            protected boolean matchesSafely(View view) {
                if (view.getBackground() instanceof ColorDrawable) {
                    final int actualColor = ((ColorDrawable) view.getBackground()).getColor();
                    return actualColor == expectedColor;
                }
                return false;
            }
        };
    }
}
