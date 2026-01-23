package com.hcaptcha.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Context;
import androidx.fragment.app.FragmentActivity;

import com.hcaptcha.sdk.journeylitics.Journeylitics;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class HCaptchaStopEventsTest {

    private static void resetJourneyliticsState() throws Exception {
        final Field startedField = Journeylitics.class.getDeclaredField("STARTED");
        startedField.setAccessible(true);
        ((AtomicBoolean) startedField.get(null)).set(false);

        final Field appField = Journeylitics.class.getDeclaredField("sApp");
        appField.setAccessible(true);
        appField.set(null, null);

        final Field defaultConfigField = Class.forName("com.hcaptcha.sdk.journeylitics.JLConfig")
                .getDeclaredField("DEFAULT");
        defaultConfigField.setAccessible(true);
        final Field configField = Journeylitics.class.getDeclaredField("sConfig");
        configField.setAccessible(true);
        configField.set(null, defaultConfigField.get(null));

        final Field sinksField = Journeylitics.class.getDeclaredField("SINKS");
        sinksField.setAccessible(true);
        ((List<?>) sinksField.get(null)).clear();

        final Field instrumentedField = Journeylitics.class.getDeclaredField("INSTRUMENTED");
        instrumentedField.setAccessible(true);
        ((Map<?, ?>) instrumentedField.get(null)).clear();
    }

    @Test
    public void stopEvents_unregisters_sink() throws Exception {
        resetJourneyliticsState();

        final FragmentActivity activity = mock(FragmentActivity.class);
        final Application app = mock(Application.class);
        when(app.getApplicationContext()).thenReturn(app);
        when(activity.getApplicationContext()).thenReturn(app);

        final HCaptchaConfig config = HCaptchaConfig.builder()
                .siteKey(HCaptchaConfigTest.MOCK_SITE_KEY)
                .userJourney(true)
                .build();

        final HCaptcha hCaptcha = HCaptcha.getClient(activity);
        try (MockedStatic<HCaptchaDialogFragment> dialogFragmentMock = mockStatic(HCaptchaDialogFragment.class)) {
            dialogFragmentMock
                    .when(() -> HCaptchaDialogFragment.newInstance(
                            any(Context.class),
                            any(HCaptchaConfig.class),
                            any(HCaptchaInternalConfig.class),
                            any(HCaptchaStateListener.class)))
                    .thenReturn(mock(HCaptchaDialogFragment.class));

            hCaptcha.setup(config);
        }

        final Field sinksField = Journeylitics.class.getDeclaredField("SINKS");
        sinksField.setAccessible(true);
        final List<?> sinks = (List<?>) sinksField.get(null);
        assertEquals(1, sinks.size());

        hCaptcha.stopEvents();

        assertEquals(0, sinks.size());
        final Field journeySinkField = HCaptcha.class.getDeclaredField("journeySink");
        journeySinkField.setAccessible(true);
        assertNull(journeySinkField.get(hCaptcha));
    }
}
