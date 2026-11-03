package com.hcaptcha.sdk.journeylitics;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class JLEvent {
    private final long timestamp;
    private final EventKind kind;
    private final String view;
    private final Object metadata;

    JLEvent(long timestamp, EventKind kind, String view, Object metadata) {
        this.timestamp = timestamp;
        this.kind = kind;
        this.view = view;
        this.metadata = metadata;
    }

    JLEvent(EventKind kind, String view, Map<String, Object> metadata) {
        this(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()), kind, view, metadata);
    }

    JLEvent(EventKind kind, String view) {
        this(kind, view, new java.util.HashMap<>());
    }

    /**
     * Returns the event payload with a UNIX timestamp in seconds.
     */
    public Map<String, Object> toMap() {
        final Map<String, Object> event = new HashMap<>();
        event.put("ts", timestamp);
        event.put("k", kind == null ? null : kind.getValue());
        event.put("v", view);
        event.put("m", metadata);
        return event;
    }
}
