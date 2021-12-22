package com.hcaptcha.sdk;

import android.os.Bundle;
import android.webkit.WebView;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static com.hcaptcha.sdk.AssertUtil.waitToBeDisplayed;
import static com.hcaptcha.sdk.HCaptchaDialogFragment.KEY_CONFIG;
import static com.hcaptcha.sdk.HCaptchaDialogFragment.KEY_LISTENER;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;


@RunWith(AndroidJUnit4.class)
public class HCaptchaDialogFragmentTest {

    public FragmentScenario<HCaptchaDialogFragment> getTestScenario() {
        final HCaptchaConfig hCaptchaConfig = HCaptchaConfig.builder()
                .siteKey("10000000-ffff-ffff-ffff-000000000001")
                .endpoint("https://js.hcaptcha.com/1/api.js")
                .locale("en")
                .size(HCaptchaSize.INVISIBLE)
                .theme(HCaptchaTheme.LIGHT)
                .build();
        final Bundle args = new Bundle();
        args.putSerializable(KEY_CONFIG, hCaptchaConfig);
        args.putParcelable(KEY_LISTENER, new HCaptchaDialogListener() {
            @Override
            void onSuccess(HCaptchaTokenResponse hCaptchaTokenResponse) {
            }

            @Override
            void onFailure(HCaptchaException hCaptchaException) {
            }
        });
        return FragmentScenario.launch(HCaptchaDialogFragment.class, args);
    }

    @Test
    public void loader_is_visible_while_webview_is_loading() {
        getTestScenario();
        onView(withId(R.id.loadingContainer)).check(matches(isDisplayed()));
        onView(withId(R.id.webView)).check(matches(not(isDisplayed())));
    }

    @Test
    public void webview_is_visible_after_loading() {
        final FragmentScenario<HCaptchaDialogFragment> scenario = getTestScenario();
        scenario.onFragment(new FragmentScenario.FragmentAction<HCaptchaDialogFragment>() {
            @Override
            public void perform(@NotNull HCaptchaDialogFragment fragment) {
                assertNotNull(fragment.getDialog());
                WebView.setWebContentsDebuggingEnabled(true);
                fragment.onLoaded();
            }
        });
        onView(withId(R.id.loadingContainer)).check(matches(not(isDisplayed())));
        onView(withId(R.id.webView)).check(matches(isDisplayed()));
        onView(isRoot()).perform(waitToBeDisplayed(R.id.webView, 1000));
    }

}
