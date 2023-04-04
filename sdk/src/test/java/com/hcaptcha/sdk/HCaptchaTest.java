package com.hcaptcha.sdk;

import static com.hcaptcha.sdk.HCaptcha.META_SITE_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;

import com.hcaptcha.sdk.tasks.OnFailureListener;
import com.hcaptcha.sdk.tasks.OnOpenListener;
import com.hcaptcha.sdk.tasks.OnSuccessListener;
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


@RunWith(MockitoJUnitRunner.class)
public class HCaptchaTest {
    private static final String TEST_PACKAGE_NAME = "com.hcaptcha.sdk.test";

    @Mock
    FragmentActivity fragmentActivity;

    @Mock
    PackageManager packageManager;

    @Mock
    HCaptchaDialogFragment fragment;

    @Mock
    OnSuccessListener<HCaptchaTokenResponse> onSuccessListener;

    @Mock
    OnFailureListener onFailureListener;

    @Mock
    OnOpenListener onOpenListener;

    @Captor
    ArgumentCaptor<HCaptchaConfig> hCaptchaConfigCaptor;

    MockedStatic<HCaptchaDialogFragment> dialogFragmentMock;

    final HCaptchaConfig config = HCaptchaConfig.builder()
            .siteKey(HCaptchaConfigTest.MOCK_SITE_KEY)
            .size(HCaptchaSize.NORMAL)
            .loading(true)
            .build();

    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);
        dialogFragmentMock = mockStatic(HCaptchaDialogFragment.class);
        dialogFragmentMock
                .when(() -> HCaptchaDialogFragment.newInstance(
                        any(HCaptchaConfig.class),
                        any(HCaptchaInternalConfig.class),
                        any(HCaptchaStateListener.class)))
                .thenReturn(fragment);
    }

    @After
    public void release() {
        dialogFragmentMock.close();
    }

    @Test
    public void test_client_creation_via_activity() {
        assertNotNull(HCaptcha.getClient(fragmentActivity));
    }

    @Test
    public void test_fragment_creation_via_activity() {
        assertNotNull(HCaptcha.getClient(fragmentActivity).setup(HCaptchaConfigTest.MOCK_SITE_KEY));

        dialogFragmentMock.verify(() -> HCaptchaDialogFragment.newInstance(
                hCaptchaConfigCaptor.capture(),
                any(HCaptchaInternalConfig.class),
                any(HCaptchaStateListener.class)));
    }

    @Test
    public void test_site_key_from_metadata() throws Exception {
        final ApplicationInfo applicationInfo = mock(ApplicationInfo.class);
        final Bundle bundle = mock(Bundle.class);
        when(bundle.getString(META_SITE_KEY)).thenReturn(HCaptchaConfigTest.MOCK_SITE_KEY);
        bundle.putString(META_SITE_KEY, HCaptchaConfigTest.MOCK_SITE_KEY);
        applicationInfo.metaData = bundle;

        when(fragmentActivity.getPackageName()).thenReturn(TEST_PACKAGE_NAME);
        when(fragmentActivity.getPackageManager()).thenReturn(packageManager);
        when(packageManager.getApplicationInfo(TEST_PACKAGE_NAME, PackageManager.GET_META_DATA))
                .thenReturn(applicationInfo);

        assertNotNull(HCaptcha.getClient(fragmentActivity).setup());
    }

    @Test
    public void test_verify_with_hcaptcha_passes_site_key_as_config() {
        final String siteKey = HCaptchaConfigTest.MOCK_SITE_KEY;
        final HCaptcha hCaptcha = HCaptcha.getClient(fragmentActivity);

        dialogFragmentMock.verify(never(), () ->
                HCaptchaDialogFragment.newInstance(
                        any(HCaptchaConfig.class),
                        any(HCaptchaInternalConfig.class),
                        any(HCaptchaStateListener.class)));

        hCaptcha.verifyWithHCaptcha(siteKey);

        dialogFragmentMock.verify(() ->
                HCaptchaDialogFragment.newInstance(
                        hCaptchaConfigCaptor.capture(),
                        any(HCaptchaInternalConfig.class),
                        any(HCaptchaStateListener.class)));
        verify(fragment).startVerification(fragmentActivity);

        final HCaptchaConfig capturedConfig = hCaptchaConfigCaptor.getValue();
        assertNotNull(capturedConfig);
        assertEquals(siteKey, capturedConfig.getSiteKey());

        // Rest of params must be the defaults
        final String locale = Locale.getDefault().getLanguage();
        assertEquals(HCaptchaSize.INVISIBLE, capturedConfig.getSize());
        assertEquals(HCaptchaTheme.LIGHT, capturedConfig.getTheme());
        assertNull(capturedConfig.getRqdata());
        assertEquals(locale, capturedConfig.getLocale());
        assertEquals("https://js.hcaptcha.com/1/api.js", capturedConfig.getApiEndpoint());
    }

    @Test
    public void test_verify_site_key_arg_has_priority_over_metadata() throws Exception {
        HCaptcha.getClient(fragmentActivity)
                .setup(HCaptchaConfigTest.MOCK_SITE_KEY)
                .verifyWithHCaptcha();

        verify(packageManager, never()).getApplicationInfo(any(String.class), anyInt());
        dialogFragmentMock.verify(() ->
                HCaptchaDialogFragment.newInstance(
                        hCaptchaConfigCaptor.capture(),
                        any(HCaptchaInternalConfig.class),
                        any(HCaptchaStateListener.class)));

        final HCaptchaConfig capturedConfig = hCaptchaConfigCaptor.getValue();
        assertEquals(HCaptchaConfigTest.MOCK_SITE_KEY, capturedConfig.getSiteKey());
    }

    @Test
    public void test_setup_config() throws Exception {
        HCaptcha.getClient(fragmentActivity)
                .setup(config)
                .verifyWithHCaptcha();

        verify(packageManager, never()).getApplicationInfo(any(String.class), anyInt());
        dialogFragmentMock.verify(() ->
                HCaptchaDialogFragment.newInstance(
                        hCaptchaConfigCaptor.capture(),
                        any(HCaptchaInternalConfig.class),
                        any(HCaptchaStateListener.class)));

        assertEquals(config, hCaptchaConfigCaptor.getValue());
    }

    @Test
    public void test_verify_config_has_priority_over_setup_config() throws Exception {
        final HCaptchaConfig verifyConfig = HCaptchaConfig.builder()
                .siteKey(HCaptchaConfigTest.MOCK_SITE_KEY + "-on-verify")
                .size(HCaptchaSize.INVISIBLE)
                .loading(false)
                .build();

        HCaptcha.getClient(fragmentActivity)
                .setup(config)
                .verifyWithHCaptcha(verifyConfig);

        verify(packageManager, never()).getApplicationInfo(any(String.class), anyInt());
        dialogFragmentMock.verify(times(2), () ->
                HCaptchaDialogFragment.newInstance(
                        hCaptchaConfigCaptor.capture(),
                        any(HCaptchaInternalConfig.class),
                        any(HCaptchaStateListener.class)));

        assertEquals(verifyConfig, hCaptchaConfigCaptor.getValue());
    }

    @Test
    public void test_verify_site_key_has_priority_over_setup_config() throws Exception {
        final String siteKey = HCaptchaConfigTest.MOCK_SITE_KEY + "-on-verify";
        HCaptcha.getClient(fragmentActivity)
                .setup(config)
                .verifyWithHCaptcha(siteKey);

        verify(packageManager, never()).getApplicationInfo(any(String.class), anyInt());
        dialogFragmentMock.verify(times(2), () ->
                HCaptchaDialogFragment.newInstance(
                        hCaptchaConfigCaptor.capture(),
                        any(HCaptchaInternalConfig.class),
                        any(HCaptchaStateListener.class)));

        assertEquals(siteKey, hCaptchaConfigCaptor.getValue().getSiteKey());
    }

    @Test
    public void test_reset() {
        HCaptcha.getClient(fragmentActivity).setup(config).reset();
        verify(fragment).reset();
    }

    @Test
    public void test_remove_listener() {
        HCaptcha.getClient(fragmentActivity)
                .addOnSuccessListener(onSuccessListener)
                .removeOnSuccessListener(onSuccessListener);
    }

    @Test
    public void test_remove_non_existing_listener() {
        HCaptcha.getClient(fragmentActivity)
                .removeOnSuccessListener(onSuccessListener);
    }

    @Test
    public void test_clear_all_listener() {
        HCaptcha.getClient(fragmentActivity)
                .addOnSuccessListener(onSuccessListener)
                .addOnFailureListener(onFailureListener)
                .addOnOpenListener(onOpenListener)
                .removeAllListeners();
    }
}
