package com.hcaptcha.sdk.journeylitics;

import com.fasterxml.jackson.annotation.JsonProperty;

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
     * UNIX timestamp (seconds).
     */
    @JsonProperty("ts")
    long getTimestamp() {
        return timestamp;
    }

    @JsonProperty("k")
    EventKind getKind() {
        return kind;
    }

    @JsonProperty("v")
    String getView() {
        return view;
    }

    @JsonProperty("m")
    Object getMetadata() {
        return metadata;
    }
}

