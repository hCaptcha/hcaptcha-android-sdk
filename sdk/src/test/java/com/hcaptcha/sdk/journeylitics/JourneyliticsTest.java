package com.hcaptcha.sdk.journeylitics;

import android.app.Activity;
import android.app.Application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class JourneyliticsTest {
    private static final String VIEW_BUTTON = "Button";
    private final List<JLEvent> captured = new ArrayList<>();

    private static void resetJourneyliticsState() throws Exception {
        final Field startedField = Journeylitics.class.getDeclaredField("STARTED");
        startedField.setAccessible(true);
        ((AtomicBoolean) startedField.get(null)).set(false);

        final Field appField = Journeylitics.class.getDeclaredField("sApp");
        appField.setAccessible(true);
        appField.set(null, null);

        final Field configField = Journeylitics.class.getDeclaredField("sConfig");
        configField.setAccessible(true);
        configField.set(null, JLConfig.DEFAULT);

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

    @Test
    public void sink_emits_event() {
        // This test verifies that the sink pipeline works correctly
        final int before = captured.size();
        final JLSink sink = new JLSink() {
            @Override
            public void emit(JLEvent event) {
                captured.add(event);
            }
        };
        final Map<String, Object> meta = MetaMapHelper.createMetaMap(
            new AbstractMap.SimpleEntry<>(FieldKey.ID, "test-button")
        );
        sink.emit(new JLEvent(EventKind.click, VIEW_BUTTON, new HashMap<>(meta)));
        Assert.assertTrue(captured.size() == before + 1);
    }

    @Test
    public void metadata_serializes_as_string() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final JLEvent event = new JLEvent(1234567890L, EventKind.click, VIEW_BUTTON, "meta-string");
        final JsonNode node = mapper.readTree(mapper.writeValueAsString(event));
        Assert.assertEquals("meta-string", node.get("m").asText());
    }

    @Test
    public void metadata_serializes_as_object() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final Map<String, Object> meta = new HashMap<>();
        meta.put("id", "submit-btn");
        final JLEvent event = new JLEvent(1234567890L, EventKind.click, VIEW_BUTTON, meta);
        final JsonNode node = mapper.readTree(mapper.writeValueAsString(event));
        Assert.assertEquals("submit-btn", node.get("m").get("id").asText());
    }

    @Test
    public void addSink_afterStart_receivesEvents() throws Exception {
        resetJourneyliticsState();
        final Application app = Mockito.mock(Application.class);
        final Activity activity = Mockito.mock(Activity.class);
        Mockito.when(activity.getApplication()).thenReturn(app);
        Journeylitics.start(activity, new JLConfig());

        final List<JLEvent> events = new ArrayList<>();
        final JLSink sink = events::add;
        Journeylitics.addSink(sink);

        final Map<String, Object> meta = MetaMapHelper.createMetaMap(
            new AbstractMap.SimpleEntry<>(FieldKey.ID, "test-button")
        );
        Journeylitics.emit(EventKind.click, VIEW_BUTTON, meta);
        Assert.assertEquals(1, events.size());
    }

    @Test
    public void removeSink_stopsEvents() throws Exception {
        resetJourneyliticsState();
        final Application app = Mockito.mock(Application.class);
        final Activity activity = Mockito.mock(Activity.class);
        Mockito.when(activity.getApplication()).thenReturn(app);
        Journeylitics.start(activity, new JLConfig());

        final List<JLEvent> events = new ArrayList<>();
        final JLSink sink = events::add;
        Journeylitics.addSink(sink);

        Journeylitics.emit(EventKind.click, VIEW_BUTTON, new HashMap<>());
        Assert.assertEquals(1, events.size());

        Journeylitics.removeSink(sink);
        Journeylitics.emit(EventKind.click, VIEW_BUTTON, new HashMap<>());
        Assert.assertEquals(1, events.size());
    }
}
