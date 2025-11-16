package com.hcaptcha.sdk.journeylitics;

import java.util.Arrays;
import java.util.List;

/**
 * Runtime configuration
 */
public class JLConfig {
    static final JLConfig DEFAULT = new JLConfig();

    private final boolean enableScreens;
    private final boolean enableClicks;
    private final boolean enableToggles;
    private final boolean enableSliders;
    private final boolean enableScrolls;
    private final boolean enableSearch;
    private final boolean enableTextInputs;
    private final List<JLSink> sinks;

    JLConfig(boolean enableScreens, boolean enableClicks, boolean enableToggles,
                   boolean enableSliders, boolean enableScrolls, boolean enableSearch,
                   boolean enableTextInputs, List<JLSink> sinks) {
        this.enableScreens = enableScreens;
        this.enableClicks = enableClicks;
        this.enableToggles = enableToggles;
        this.enableSliders = enableSliders;
        this.enableScrolls = enableScrolls;
        this.enableSearch = enableSearch;
        this.enableTextInputs = enableTextInputs;
        this.sinks = sinks;
    }

    JLConfig() {
        this(true, true, true, true, true, true, true, Arrays.asList(ConsoleSink.INSTANCE));
    }

    /**
     * Creates a config with all features enabled and a single sink
     * @param sink The sink to use for event emission
     */
    public JLConfig(JLSink sink) {
        this(true, true, true, true, true, true, true, Arrays.asList(sink));
    }

    boolean isEnableScreens() {
        return enableScreens;
    }

    boolean isEnableClicks() {
        return enableClicks;
    }

    boolean isEnableToggles() {
        return enableToggles;
    }

    boolean isEnableSliders() {
        return enableSliders;
    }

    boolean isEnableScrolls() {
        return enableScrolls;
    }

    boolean isEnableSearch() {
        return enableSearch;
    }

    boolean isEnableTextInputs() {
        return enableTextInputs;
    }

    List<JLSink> getSinks() {
        return sinks;
    }
}

