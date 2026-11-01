package com.hcaptcha.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.robolectric.Shadows.shadowOf;

import android.os.Build;
import android.os.Looper;
import android.widget.FrameLayout;
import androidx.fragment.app.FragmentActivity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(ParameterizedRobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.S_V2)
public class HCaptchaReconfigurationTest {
    private final HCaptchaRenderMode mode;
    private final boolean loaded;

    @ParameterizedRobolectricTestRunner.Parameters(name = "{0}, loaded={1}")
    public static Collection<Object[]> configurations() {
        return Arrays.asList(new Object[][] {
                {HCaptchaRenderMode.DIALOG, false}, {HCaptchaRenderMode.DIALOG, true},
                {HCaptchaRenderMode.HEADLESS, false}, {HCaptchaRenderMode.HEADLESS, true},
                {HCaptchaRenderMode.EMBEDDED, false}, {HCaptchaRenderMode.EMBEDDED, true}
        });
    }

    public HCaptchaReconfigurationTest(HCaptchaRenderMode mode, boolean loaded) {
        this.mode = mode;
        this.loaded = loaded;
    }

    @Test
    public void reconfigurationReplacesVerifierAndCompletes() throws ReflectiveOperationException {
        final FragmentActivity activity = Robolectric.buildActivity(FragmentActivity.class).setup().get();
        final FrameLayout container = new FrameLayout(activity);
        activity.setContentView(container);
        final HCaptchaConfig config = HCaptchaConfig.builder()
                .siteKey(HCaptchaConfigTest.MOCK_SITE_KEY).renderMode(mode).build();
        final HCaptcha client = HCaptcha.getClient(activity).setEmbeddedContainer(container).setup(config);
        final AtomicInteger successes = new AtomicInteger();
        final AtomicInteger failures = new AtomicInteger();
        client.addOnSuccessListener(token -> successes.incrementAndGet());
        client.addOnFailureListener(error -> failures.incrementAndGet());
        client.verifyWithHCaptcha();
        final Field verifierField = HCaptcha.class.getDeclaredField("captchaVerifier");
        verifierField.setAccessible(true);
        final IHCaptchaVerifier old = (IHCaptchaVerifier) verifierField.get(client);
        final Field helperField = old.getClass().getDeclaredField("webViewHelper");
        helperField.setAccessible(true);
        final HCaptchaWebView oldWebView = ((HCaptchaWebViewHelper) helperField.get(old)).getWebView();
        if (loaded) {
            old.onLoaded();
            activity.getSupportFragmentManager().executePendingTransactions();
            shadowOf(Looper.getMainLooper()).idle();
        }

        final HCaptchaConfig updated = config.toBuilder().theme(HCaptchaTheme.DARK).build();
        client.verifyWithHCaptcha(updated);
        activity.getSupportFragmentManager().executePendingTransactions();
        shadowOf(Looper.getMainLooper()).idle();

        final IHCaptchaVerifier current = (IHCaptchaVerifier) verifierField.get(client);
        final HCaptchaWebViewHelper helper = (HCaptchaWebViewHelper) helperField.get(current);
        assertNotSame(old, current);
        assertNull("the old WebView must be detached", oldWebView.getParent());
        assertEquals(updated.getTheme(), helper.getConfig().getTheme());
        if (mode == HCaptchaRenderMode.DIALOG) {
            assertSame(current, activity.getSupportFragmentManager()
                    .findFragmentByTag(HCaptchaDialogFragment.class.getSimpleName()));
            assertSame(helper.getWebView(), ((HCaptchaDialogFragment) current)
                    .requireView().findViewById(R.id.webView));
        } else if (mode == HCaptchaRenderMode.EMBEDDED) {
            assertEquals(1, container.getChildCount());
            assertSame(helper.getWebView(), container.getChildAt(0));
        } else {
            assertNotNull(helper.getWebView().getParent());
        }
        old.onSuccess("stale-token");
        old.onFailure(new HCaptchaException(HCaptchaError.ERROR));
        current.onSuccess("fresh-token");
        assertEquals(1, successes.get());
        assertEquals(0, failures.get());
        client.destroy();
    }
}
