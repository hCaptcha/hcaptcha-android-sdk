package com.hcaptcha.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.os.Build;
import android.os.Looper;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

// Recovery once the WebView is gone, with a real FragmentManager instead of mocks (#214).
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.S_V2)
public class HCaptchaWebViewGoneRecoveryTest {
    private final HCaptchaConfig config = HCaptchaConfig.builder()
            .siteKey(HCaptchaConfigTest.MOCK_SITE_KEY)
            .build();
    private final HCaptchaException exception = new HCaptchaException(HCaptchaError.ERROR);

    private FragmentActivity activity;
    private FragmentManager fragmentManager;

    @Before
    public void init() {
        activity = Robolectric.buildActivity(FragmentActivity.class).setup().get();
        fragmentManager = activity.getSupportFragmentManager();
    }

    @Test
    public void dialogVerifyFromFailureListenerShowsReplacement() {
        final HCaptcha hCaptcha = HCaptcha.getClient(activity).setup(config);
        hCaptcha.verifyWithHCaptcha();
        commitPending();
        final HCaptchaDialogFragment dead = (HCaptchaDialogFragment) verifierOf(hCaptcha);
        assertTrue(dead.isAdded());
        retryOnceFromFailureListener(hCaptcha);

        dead.onWebViewGone(exception);
        commitPending();

        final IHCaptchaVerifier replacement = verifierOf(hCaptcha);
        assertNotSame(dead, replacement);
        assertTrue("the replacement dialog should be shown",
                ((HCaptchaDialogFragment) replacement).isAdded());
    }

    @Test
    public void dialogGoneBeforeShowCommitsIsNeverAdded() {
        final HCaptcha hCaptcha = HCaptcha.getClient(activity).setup(config);
        hCaptcha.verifyWithHCaptcha();
        final HCaptchaDialogFragment dead = (HCaptchaDialogFragment) verifierOf(hCaptcha);
        assertFalse(dead.isAdded());

        dead.onWebViewGone(exception);
        commitPending();

        assertFalse("the dead dialog should not be added", dead.isAdded());
    }

    @Test
    public void staleWebViewGoneKeepsReplacement() {
        final HCaptchaConfig headless = config.toBuilder().renderMode(HCaptchaRenderMode.HEADLESS).build();
        final HCaptcha hCaptcha = HCaptcha.getClient(activity).setup(headless);
        // Reset before load keeps this WebView alive until it loads
        final IHCaptchaVerifier stale = verifierOf(hCaptcha);
        hCaptcha.reset();
        hCaptcha.setup(headless);
        final IHCaptchaVerifier current = verifierOf(hCaptcha);
        final AtomicInteger failures = retryOnceFromFailureListener(hCaptcha);

        // The renderer is shared, so both WebViews hear about the same crash
        current.onWebViewGone(exception);
        final IHCaptchaVerifier replacement = verifierOf(hCaptcha);
        assertNotNull(replacement);
        assertNotSame(current, replacement);
        stale.onWebViewGone(exception);

        assertSame("a stale callback should keep the replacement", replacement, verifierOf(hCaptcha));
        assertEquals("the crash should be reported once", 1, failures.get());
    }

    @Test
    public void dialogVerifyFromFailureListenerShowsDialogAgain() {
        // Not about the WebView being gone: an ordinary failure, retried from the listener
        final HCaptcha hCaptcha = HCaptcha.getClient(activity).setup(config);
        hCaptcha.verifyWithHCaptcha();
        commitPending();
        final HCaptchaDialogFragment dialog = (HCaptchaDialogFragment) verifierOf(hCaptcha);
        retryOnceFromFailureListener(hCaptcha);

        dialog.onFailure(new HCaptchaException(HCaptchaError.CHALLENGE_CLOSED));
        commitPending();

        assertTrue("the dialog should be shown again", dialog.isAdded());
    }

    private AtomicInteger retryOnceFromFailureListener(final HCaptcha client) {
        final AtomicInteger failures = new AtomicInteger();
        client.addOnFailureListener(e -> {
            if (failures.incrementAndGet() == 1) {
                client.verifyWithHCaptcha();
            }
        });
        return failures;
    }

    private void commitPending() {
        fragmentManager.executePendingTransactions();
        shadowOf(Looper.getMainLooper()).idle();
    }

    private static IHCaptchaVerifier verifierOf(final HCaptcha client) {
        try {
            final Field field = HCaptcha.class.getDeclaredField("captchaVerifier");
            field.setAccessible(true);
            return (IHCaptchaVerifier) field.get(client);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
