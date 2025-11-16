package com.hcaptcha.sdk.journeylitics;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class JLEvent {
    private final long timestampMs;
    private final EventKind kind;
    private final String view;
    private final Map<String, Object> metadata;

    JLEvent(long timestampMs, EventKind kind, String view, Map<String, Object> metadata) {
        this.timestampMs = timestampMs;
        this.kind = kind;
        this.view = view;
        this.metadata = metadata;
    }

    JLEvent(EventKind kind, String view, Map<String, Object> metadata) {
        this(System.currentTimeMillis(), kind, view, metadata);
    }

    JLEvent(EventKind kind, String view) {
        this(kind, view, new java.util.HashMap<>());
    }

    @JsonProperty("ts")
    long getTimestampMs() {
        return timestampMs;
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
    Map<String, Object> getMetadata() {
        return metadata;
    }
}

