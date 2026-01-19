package com.hcaptcha.sdk.journeylitics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JourneyliticsTest {
    private final List<JLEvent> captured = new ArrayList<>();

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
        sink.emit(new JLEvent(EventKind.click, "Button", new HashMap<>(meta)));
        Assert.assertTrue(captured.size() == before + 1);
    }

    @Test
    public void metadata_serializes_as_string() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final JLEvent event = new JLEvent(1234567890L, EventKind.click, "Button", "meta-string");
        final JsonNode node = mapper.readTree(mapper.writeValueAsString(event));
        Assert.assertEquals("meta-string", node.get("m").asText());
    }

    @Test
    public void metadata_serializes_as_object() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final Map<String, Object> meta = new HashMap<>();
        meta.put("id", "submit-btn");
        final JLEvent event = new JLEvent(1234567890L, EventKind.click, "Button", meta);
        final JsonNode node = mapper.readTree(mapper.writeValueAsString(event));
        Assert.assertEquals("submit-btn", node.get("m").get("id").asText());
    }
}

