package com.hcaptcha.sdk.journeylitics;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(Parameterized.class)
public class JLEventTest {
    @Parameterized.Parameter
    public EventKind kind;

    @Parameterized.Parameter(1)
    public Object metadata;

    @Parameterized.Parameters(name = "{0}: {1}")
    public static List<Object[]> events() {
        final List<Object[]> events = new ArrayList<>();
        for (EventKind kind : EventKind.values()) {
            for (Object metadata : new Object[] {"meta-string", Collections.singletonMap("id", "submit-btn"), null}) {
                events.add(new Object[] {kind, metadata});
            }
        }
        return events;
    }

    @Test
    public void preserves_event_payload() throws JSONException {
        final JLEvent event = new JLEvent(1, kind, "Button", metadata);
        final JSONObject expected = new JSONObject()
                .put("ts", 1)
                .put("k", kind.name())
                .put("v", "Button")
                .put("m", metadata == null ? JSONObject.NULL : JSONObject.wrap(metadata));
        JSONAssert.assertEquals(expected, new JSONObject(event.toMap()), true);
    }
}
