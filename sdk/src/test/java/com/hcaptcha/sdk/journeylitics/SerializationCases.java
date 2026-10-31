package com.hcaptcha.sdk.journeylitics;

import com.hcaptcha.sdk.HCaptchaConfig;
import com.hcaptcha.sdk.HCaptchaOrientation;
import com.hcaptcha.sdk.HCaptchaRenderMode;
import com.hcaptcha.sdk.HCaptchaSize;
import com.hcaptcha.sdk.HCaptchaTheme;
import com.hcaptcha.sdk.HCaptchaVerifyParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shared inputs for the pre-change Jackson baseline and the replacement serializer. */
public final class SerializationCases {
    private static final int PHONE_PREFIX = 1;
    private static final int PHONE_NUMBER = 2;
    private static final int RQDATA = 4;
    private static final int JOURNEY = 8;
    private static final int PARAM_COMBINATIONS = 16;
    private static final String TEXT = "quote\"\\\n\r\t\b\f\u0000\u2028\u2029😀</script>";

    private SerializationCases() {
    }

    public static Map<String, Object> values() {
        final Map<String, Object> cases = new LinkedHashMap<>();
        final HCaptchaConfig config = HCaptchaConfig.builder().siteKey("site-key").locale("en").build();
        cases.put("default-config", config);
        for (HCaptchaSize size : HCaptchaSize.values()) {
            for (HCaptchaTheme theme : HCaptchaTheme.values()) {
                for (HCaptchaOrientation orientation : HCaptchaOrientation.values()) {
                    for (HCaptchaRenderMode mode : HCaptchaRenderMode.values()) {
                        cases.put("config-" + size + "-" + theme + "-" + orientation + "-" + mode,
                                config.toBuilder().size(size).theme(theme).orientation(orientation)
                                        .renderMode(mode).build());
                    }
                }
            }
        }
        cases.put("full-config", config.toBuilder().siteKey(TEXT).sentry(false).loading(false)
                .hideDialog(true).rqdata(TEXT).jsSrc("https://example.com/api.js")
                .endpoint("https://example.com/api").reportapi("https://example.com/report")
                .assethost("https://example.com/assets").imghost("https://example.com/images")
                .host("example.com").customTheme("{\"primary\":\"#fff\"}").resetOnTimeout(true)
                .tokenExpiration(Long.MAX_VALUE).diagnosticLog(true).disableHardwareAcceleration(false)
                .userJourney(true).build());
        cases.put("nullable-config", config.toBuilder().sentry(null).loading(null).hideDialog(null)
                .jsSrc(null).locale(null).size(null).orientation(null).theme(null).renderMode(null)
                .resetOnTimeout(null).retryPredicate(null).diagnosticLog(null).userJourney(null).build());

        final List<JLEvent> events = new ArrayList<>();
        for (EventKind kind : EventKind.values()) {
            for (Object metadata : Arrays.asList(TEXT, Collections.singletonMap("nested",
                    Arrays.asList(TEXT, null, true, 1)), null)) {
                events.add(new JLEvent(1, kind, "Button", metadata));
            }
        }
        for (int mask = 0; mask < PARAM_COMBINATIONS; mask++) {
            cases.put("verify-" + mask, HCaptchaVerifyParams.builder()
                    .phonePrefix((mask & PHONE_PREFIX) == 0 ? null : "44")
                    .phoneNumber((mask & PHONE_NUMBER) == 0 ? null : "+44123456789")
                    .rqdata((mask & RQDATA) == 0 ? null : TEXT)
                    .userJourney((mask & JOURNEY) == 0 ? null : events).build());
        }
        cases.put("verify-string-journey", HCaptchaVerifyParams.builder().userJourney(TEXT).build());
        return cases;
    }
}
