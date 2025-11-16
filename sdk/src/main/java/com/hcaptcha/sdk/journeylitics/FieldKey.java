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
    ID("i"),
    SCREEN("s"),
    ACTION("a"),
    VALUE("val"),
    X("x"),
    Y("y"),
    INDEX("idx"),
    SECTION("sect"),
    ITEM("it"),
    TARGET("t"),
    CONTROL("c"),
    GESTURE("g"),
    STATE("st"),
    TAPS("tap"),
    COMPOSE("comp");

    private final String jsonKey;

    FieldKey(String jsonKey) {
        this.jsonKey = jsonKey;
    }

    String getJsonKey() {
        return jsonKey;
    }
}

