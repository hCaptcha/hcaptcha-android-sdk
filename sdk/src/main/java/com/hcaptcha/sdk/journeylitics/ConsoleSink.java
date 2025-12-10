package com.hcaptcha.sdk.journeylitics;

import java.util.Map;

/**
 * Default sink printing compact JSON-like line
 */
final class ConsoleSink implements JLSink {
    static final ConsoleSink INSTANCE = new ConsoleSink();

    private ConsoleSink() {}

    @Override
    public void emit(JLEvent event) {
        final Map<String, String> parts = new java.util.HashMap<>();
        parts.put(FieldKey.KIND.getJsonKey(), event.getKind().getValue());
        parts.put(FieldKey.VIEW.getJsonKey(), event.getView());
        parts.put(FieldKey.TIMESTAMP.getJsonKey(), String.valueOf(event.getTimestampMs()));
        parts.put(FieldKey.META.getJsonKey(), event.getMetadata().toString());

        final StringBuilder body = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : parts.entrySet()) {
            if (!first) {
                body.append(',');
            }
            body.append('"').append(entry.getKey()).append("\":\"").append(entry.getValue()).append('"');
            first = false;
        }
        System.out.println("{" + body.toString() + "}");
    }
}

