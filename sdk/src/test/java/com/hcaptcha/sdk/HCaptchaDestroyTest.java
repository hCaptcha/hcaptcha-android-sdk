package com.hcaptcha.sdk;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.app.Activity;

import org.junit.Test;

import java.lang.reflect.Field;

/**
 * Unit test to verify that HCaptcha.destroy() calls underlying verifier.destroy()
 * and clears the cached verifier reference.
 */
public class HCaptchaDestroyTest {

    @Test
    public void destroy_invokes_verifier_and_clears_reference() throws Exception {
        final Activity activity = mock(Activity.class);
        final HCaptcha hCaptcha = HCaptcha.getClient(activity);

        // Inject a mocked verifier via reflection
        final IHCaptchaVerifier verifier = mock(IHCaptchaVerifier.class);
        final Field f = HCaptcha.class.getDeclaredField("captchaVerifier");
        f.setAccessible(true);
        f.set(hCaptcha, verifier);

        // Act
        hCaptcha.destroy();

        // Assert
        verify(verifier).destroy();
        assertNull(f.get(hCaptcha));
    }
}

