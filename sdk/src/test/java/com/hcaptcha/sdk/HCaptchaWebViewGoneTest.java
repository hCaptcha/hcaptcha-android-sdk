package com.hcaptcha.sdk;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.os.Build;
import android.view.ViewGroup;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.MockedConstruction;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

// A dead WebView must be released and reported, never retried (#214).
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.S_V2)
public class HCaptchaWebViewGoneTest {
    private final HCaptchaConfig config = HCaptchaConfig.builder()
            .siteKey(HCaptchaConfigTest.MOCK_SITE_KEY)
            .build();
    private final HCaptchaInternalConfig internalConfig = HCaptchaInternalConfig.builder().build();
    private final HCaptchaStateListener listener = mock(HCaptchaStateListener.class);
    private final HCaptchaException exception = new HCaptchaException(HCaptchaError.ERROR);

    private Activity activity;
    private MockedConstruction<HCaptchaWebViewHelper> helperMock;

    @Before
    public void init() {
        activity = Robolectric.buildActivity(Activity.class).setup().get();
        // A predicate that retries everything: the dead WebView must still not be retried
        helperMock = mockConstruction(HCaptchaWebViewHelper.class, (helper, context) ->
                when(helper.shouldRetry(any(HCaptchaException.class))).thenReturn(true));
    }

    @After
    public void release() {
        helperMock.close();
    }

    private void assertReleasedWithoutRetry(final IHCaptchaVerifier verifier) {
        verifier.onWebViewGone(exception);

        final HCaptchaWebViewHelper helper = helperMock.constructed().get(0);
        verify(helper).destroy();
        verify(helper, never()).resetAndExecute(any());
        // Released before the failure, so a verify from the failure listener starts fresh
        final InOrder order = inOrder(listener);
        order.verify(listener).onWebViewGone();
        order.verify(listener).onFailure(exception);
    }

    @Test
    public void dialogIsReleasedWithoutRetry() {
        assertReleasedWithoutRetry(
                HCaptchaDialogFragment.newInstance(activity, config, internalConfig, listener));
    }

    @Test
    public void embeddedIsReleasedWithoutRetry() {
        assertReleasedWithoutRetry(new HCaptchaEmbeddedView(
                activity, config, internalConfig, listener, mock(ViewGroup.class)));
    }

    @Test
    public void headlessIsReleasedWithoutRetry() {
        assertReleasedWithoutRetry(
                new HCaptchaHeadlessWebView(activity, config, internalConfig, listener));
    }
}
