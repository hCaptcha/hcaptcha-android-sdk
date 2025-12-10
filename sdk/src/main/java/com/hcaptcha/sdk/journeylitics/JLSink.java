package com.hcaptcha.sdk.journeylitics;

/**
 * Sink interface receiving events
 */
@FunctionalInterface
public interface JLSink {
    void emit(JLEvent event);
}

