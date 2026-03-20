package com.hcaptcha.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import androidx.fragment.app.FragmentActivity;

import com.hcaptcha.sdk.journeylitics.InMemorySink;
import com.hcaptcha.sdk.journeylitics.Journeylitics;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class HCaptchaJourneyLifecycleTest {
    private static final int LISTENER_ARG_INDEX = 3;

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

        final Field scrollEventField = Journeylitics.class.getDeclaredField("LAST_SCROLL_EVENT_AT");
        scrollEventField.setAccessible(true);
        ((Map<?, ?>) scrollEventField.get(null)).clear();
    }

    private static FragmentActivity createActivity() {
        final FragmentActivity activity = mock(FragmentActivity.class);
        final Application app = mock(Application.class);
        when(app.getApplicationContext()).thenReturn(app);
        when(activity.getApplicationContext()).thenReturn(app);
        when(activity.getApplication()).thenReturn(app);
        return activity;
    }

    private static InMemorySink getJourneySink(HCaptcha hcaptchaClient) throws Exception {
        final Field journeySinkField = HCaptcha.class.getDeclaredField("journeySink");
        journeySinkField.setAccessible(true);
        return (InMemorySink) journeySinkField.get(hcaptchaClient);
    }

    @SuppressWarnings("unchecked")
    private static void emitClickEvent() throws Exception {
        final Class<?> eventKindClass = Class.forName("com.hcaptcha.sdk.journeylitics.EventKind");
        final Object clickKind = Enum.valueOf((Class<Enum>) eventKindClass, "click");
        final Method emit = Journeylitics.class.getMethod(
                "emit", eventKindClass, String.class, Map.class);
        emit.invoke(null, clickKind, "Button", new HashMap<String, Object>());
    }

    @Test
    public void clearOnSuccess_clearsBufferedEvents() throws Exception {
        resetJourneyliticsState();
        final FragmentActivity activity = createActivity();
        final HCaptchaConfig config = HCaptchaConfig.builder()
                .siteKey(HCaptchaConfigTest.MOCK_SITE_KEY)
                .userJourney(true)
                .build();

        final AtomicReference<HCaptchaStateListener> listenerRef = new AtomicReference<>();
        final HCaptchaDialogFragment verifier = mock(HCaptchaDialogFragment.class);
        doAnswer(invocation -> {
            final HCaptchaStateListener listener = listenerRef.get();
            if (listener != null) {
                listener.onSuccess("token-1");
            }
            return null;
        }).when(verifier).startVerification(any(Activity.class), any(HCaptchaVerifyParams.class));

        final HCaptcha hCaptcha = HCaptcha.getClient(activity);
        try (MockedStatic<HCaptchaDialogFragment> dialogFragmentMock = mockStatic(HCaptchaDialogFragment.class)) {
            dialogFragmentMock
                    .when(() -> HCaptchaDialogFragment.newInstance(
                            any(Context.class),
                            any(HCaptchaConfig.class),
                            any(HCaptchaInternalConfig.class),
                            any(HCaptchaStateListener.class)))
                    .thenAnswer(invocation -> {
                        listenerRef.set(invocation.getArgument(LISTENER_ARG_INDEX));
                        return verifier;
                    });
            hCaptcha.setup(config);
        }

        emitClickEvent();
        final InMemorySink sink = getJourneySink(hCaptcha);
        assertNotNull(sink);
        assertEquals(1, sink.getEvents().size());

        hCaptcha.verifyWithHCaptcha();
        assertTrue(sink.getEvents().isEmpty());
    }

    @Test
    public void sequence_withoutDestroy_keepsTrackingBetweenTokens() throws Exception {
        resetJourneyliticsState();
        final FragmentActivity activity = createActivity();
        final HCaptchaConfig config = HCaptchaConfig.builder()
                .siteKey(HCaptchaConfigTest.MOCK_SITE_KEY)
                .userJourney(true)
                .build();

        final AtomicReference<HCaptchaStateListener> listenerRef = new AtomicReference<>();
        final HCaptchaDialogFragment verifier = mock(HCaptchaDialogFragment.class);
        doAnswer(invocation -> {
            final HCaptchaStateListener listener = listenerRef.get();
            if (listener != null) {
                listener.onSuccess("token");
            }
            return null;
        }).when(verifier).startVerification(any(Activity.class), any(HCaptchaVerifyParams.class));

        final HCaptcha hCaptcha = HCaptcha.getClient(activity);
        try (MockedStatic<HCaptchaDialogFragment> dialogFragmentMock = mockStatic(HCaptchaDialogFragment.class)) {
            dialogFragmentMock
                    .when(() -> HCaptchaDialogFragment.newInstance(
                            any(Context.class),
                            any(HCaptchaConfig.class),
                            any(HCaptchaInternalConfig.class),
                            any(HCaptchaStateListener.class)))
                    .thenAnswer(invocation -> {
                        listenerRef.set(invocation.getArgument(LISTENER_ARG_INDEX));
                        return verifier;
                    });
            hCaptcha.setup(config);
        }

        final InMemorySink sink = getJourneySink(hCaptcha);
        assertNotNull(sink);

        emitClickEvent();
        assertEquals(1, sink.getEvents().size());
        hCaptcha.verifyWithHCaptcha();
        assertTrue(sink.getEvents().isEmpty());

        emitClickEvent();
        assertEquals(1, sink.getEvents().size());
        hCaptcha.verifyWithHCaptcha();
        assertTrue(sink.getEvents().isEmpty());
    }

    @Test
    public void sequence_withDestroy_requiresRestartToCaptureEvents() throws Exception {
        resetJourneyliticsState();
        final FragmentActivity activity = createActivity();
        final HCaptchaConfig config = HCaptchaConfig.builder()
                .siteKey(HCaptchaConfigTest.MOCK_SITE_KEY)
                .userJourney(true)
                .build();

        final AtomicReference<HCaptchaStateListener> listenerRef = new AtomicReference<>();
        final HCaptchaDialogFragment verifier = mock(HCaptchaDialogFragment.class);
        doAnswer(invocation -> {
            final HCaptchaStateListener listener = listenerRef.get();
            if (listener != null) {
                listener.onSuccess("token");
            }
            return null;
        }).when(verifier).startVerification(any(Activity.class), any(HCaptchaVerifyParams.class));

        final HCaptcha hCaptcha = HCaptcha.getClient(activity);
        try (MockedStatic<HCaptchaDialogFragment> dialogFragmentMock = mockStatic(HCaptchaDialogFragment.class)) {
            dialogFragmentMock
                    .when(() -> HCaptchaDialogFragment.newInstance(
                            any(Context.class),
                            any(HCaptchaConfig.class),
                            any(HCaptchaInternalConfig.class),
                            any(HCaptchaStateListener.class)))
                    .thenAnswer(invocation -> {
                        listenerRef.set(invocation.getArgument(LISTENER_ARG_INDEX));
                        return verifier;
                    });
            hCaptcha.setup(config);
        }

        final InMemorySink firstSink = getJourneySink(hCaptcha);
        assertNotNull(firstSink);
        emitClickEvent();
        assertEquals(1, firstSink.getEvents().size());
        hCaptcha.verifyWithHCaptcha();
        assertTrue(firstSink.getEvents().isEmpty());

        hCaptcha.destroy();
        emitClickEvent();
        assertTrue(firstSink.getEvents().isEmpty());

        try (MockedStatic<HCaptchaDialogFragment> dialogFragmentMock = mockStatic(HCaptchaDialogFragment.class)) {
            dialogFragmentMock
                    .when(() -> HCaptchaDialogFragment.newInstance(
                            any(Context.class),
                            any(HCaptchaConfig.class),
                            any(HCaptchaInternalConfig.class),
                            any(HCaptchaStateListener.class)))
                    .thenAnswer(invocation -> {
                        listenerRef.set(invocation.getArgument(LISTENER_ARG_INDEX));
                        return verifier;
                    });
            hCaptcha.setup(config);
        }

        final InMemorySink secondSink = getJourneySink(hCaptcha);
        assertNotNull(secondSink);
        assertNotSame(firstSink, secondSink);
        emitClickEvent();
        assertEquals(1, secondSink.getEvents().size());
        hCaptcha.verifyWithHCaptcha();
        assertTrue(secondSink.getEvents().isEmpty());
    }
}
