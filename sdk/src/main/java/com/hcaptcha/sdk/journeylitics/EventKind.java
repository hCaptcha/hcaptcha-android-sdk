package com.hcaptcha.sdk.journeylitics;

/**
 * Event kinds observed by the library
 */
enum EventKind {
    screen("screen"),
    click("click"),
    drag("drag"),
    gesture("gesture"),
    edit("edit");

    private final String value;

    EventKind(String value) {
        this.value = value;
    }

    String getValue() {
        return value;
    }
}

