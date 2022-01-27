package com.hcaptcha.sdk;

import android.view.View;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.util.HumanReadables;
import androidx.test.espresso.util.TreeIterables;
import org.hamcrest.Matcher;

import java.util.concurrent.TimeoutException;

import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.not;

public class AssertUtil {

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
                    uiController.loopMainThreadForAtLeast(50);
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
                long endTime = System.currentTimeMillis() + millis;

                do {
                    if (view.getVisibility() == View.GONE || view.getVisibility() == View.INVISIBLE) {
                        return;
                    }
                    uiController.loopMainThreadForAtLeast(50);
                } while (System.currentTimeMillis() < endTime);

                throw new PerformException.Builder()
                        .withActionDescription(this.getDescription())
                        .withViewDescription(HumanReadables.describe(view))
                        .withCause(new TimeoutException())
                        .build();
            }
        };
    }
}
