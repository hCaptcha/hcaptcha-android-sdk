package com.hcaptcha.sdk;

import static com.hcaptcha.sdk.HCaptcha.META_SITE_KEY;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Locale;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class HCaptchaTest {

    @Mock
    FragmentActivity fragmentActivity;

    @Mock
    PackageManager packageManager;

    @Mock
    FragmentManager fragmentManager;

    @Captor
    ArgumentCaptor<HCaptchaConfig> hCaptchaConfigCaptor;

    MockedStatic<HCaptchaDialogFragment> dialogFragmentMock;

    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);

        dialogFragmentMock = mockStatic(HCaptchaDialogFragment.class);
    }

    @After
    public void release() {
        dialogFragmentMock.close();
    }

    @Test
    public void test_client_creation_via_activity() {
        dialogFragmentMock.when(() ->
                    HCaptchaDialogFragment.newInstance(any(HCaptchaConfig.class), any(HCaptchaDialogListener.class))).thenReturn(mock(HCaptchaDialogFragment.class));

        assertNotNull(HCaptcha.getClient(fragmentActivity, HCaptchaConfigTest.MOCK_SITE_KEY));
    }

    @Test
    public void test_site_key_from_metadata() throws Exception {
        dialogFragmentMock
                .when(() ->
                        HCaptchaDialogFragment.newInstance(any(HCaptchaConfig.class), any(HCaptchaDialogListener.class)))
                .thenReturn(mock(HCaptchaDialogFragment.class));

        ApplicationInfo applicationInfo = mock(ApplicationInfo.class);
        Bundle bundle = mock(Bundle.class);
        when(bundle.getString(META_SITE_KEY)).thenReturn(HCaptchaConfigTest.MOCK_SITE_KEY);
        bundle.putString(META_SITE_KEY, HCaptchaConfigTest.MOCK_SITE_KEY);
        applicationInfo.metaData = bundle;

        String packageName = "com.hcaptcha.sdk.test";
        when(fragmentActivity.getPackageName()).thenReturn(packageName);
        when(fragmentActivity.getPackageManager()).thenReturn(packageManager);
        when(packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA))
                .thenReturn(applicationInfo);

        assertNotNull(HCaptcha.getClient(fragmentActivity));
    }

    @Test
    public void test_verify_with_hcaptcha_passes_site_key_as_config() {
        when(fragmentActivity.getSupportFragmentManager()).thenReturn(fragmentManager);
        HCaptchaDialogFragment fragment = mock(HCaptchaDialogFragment.class);
        dialogFragmentMock
                .when(() -> HCaptchaDialogFragment.newInstance(any(HCaptchaConfig.class), any(HCaptchaDialogListener.class)))
                .thenReturn(fragment);

        final String siteKey = HCaptchaConfigTest.MOCK_SITE_KEY;
        final HCaptcha hCaptcha = HCaptcha.getClient(fragmentActivity, siteKey);

        hCaptcha.verifyWithHCaptcha();

        dialogFragmentMock.verify(() ->
                HCaptchaDialogFragment.newInstance(hCaptchaConfigCaptor.capture(), any(HCaptchaDialogListener.class)));
        verify(fragment).show(fragmentManager, HCaptchaDialogFragment.TAG);

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
