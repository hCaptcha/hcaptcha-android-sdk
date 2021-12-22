package com.hcaptcha.sdk;

import android.content.Context;
import androidx.fragment.app.FragmentActivity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Locale;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;


@RunWith(MockitoJUnitRunner.class)
public class HCaptchaTest {

    @Mock
    FragmentActivity fragmentActivity;

    @Captor
    ArgumentCaptor<HCaptchaConfig> hCaptchaConfigCaptor;

    @Test
    public void test_client_creation_via_context_and_activity() {
        assertNotNull(HCaptcha.getClient((Context) fragmentActivity));
        assertNotNull(HCaptcha.getClient(fragmentActivity));
    }

    @Test
    public void test_verify_with_hcaptcha_passes_site_key_as_config() {
        final String siteKey = "mock-site-key";
        final HCaptcha hCaptcha = spy(HCaptcha.getClient(fragmentActivity));
        doReturn(hCaptcha).when(hCaptcha).verifyWithHCaptcha(hCaptchaConfigCaptor.capture());
        hCaptcha.verifyWithHCaptcha(siteKey);
        final HCaptchaConfig config = hCaptchaConfigCaptor.getValue();
        assertNotNull(config);
        assertEquals(siteKey, config.getSiteKey());
        // Rest of params must be the defaults
        final String locale = Locale.getDefault().getLanguage();
        assertEquals(HCaptchaSize.INVISIBLE, config.getSize());
        assertEquals(HCaptchaTheme.LIGHT, config.getTheme());
        assertNull(config.getRqdata());
        assertEquals(locale, config.getLocale());
        assertEquals("https://js.hcaptcha.com/1/api.js", config.getApiEndpoint());
    }

}
