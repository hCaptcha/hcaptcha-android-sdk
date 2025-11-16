package com.hcaptcha.sdk.journeylitics;

/**
 * Serialization enum for consistent field mapping across platforms
 * Maps readable field names to short JSON keys for minification
 */
enum FieldKey {
    // Top-level fields (always present)
    KIND("k"),
    VIEW("v"),
    TIMESTAMP("ts"),
    META("m"),

    // Meta fields (nested under meta object)
    ID("id"),
    SCREEN("sc"),
    ACTION("ac"),
    VALUE("val"),
    X("x"),
    Y("y"),
    INDEX("idx"),
    SECTION("sct"),
    ITEM("it"),
    TARGET("tt"),
    CONTROL("ct"),
    GESTURE("gt"),
    STATE("gs"),
    TAPS("tap"),
    CONTAINER_VIEW("cv"),
    LENGTH("ln"),
    COMPOSE("comp");

    private final String jsonKey;

    FieldKey(String jsonKey) {
        this.jsonKey = jsonKey;
    }

    String getJsonKey() {
        return jsonKey;
    }
}

