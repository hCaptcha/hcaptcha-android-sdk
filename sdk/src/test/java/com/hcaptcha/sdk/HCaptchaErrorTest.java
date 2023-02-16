package com.hcaptcha.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class HCaptchaErrorTest {

    @Test
    public void enum_codes() {
        assertEquals("No internet connection", HCaptchaError.NETWORK_ERROR.toString());
        assertEquals("Session Timeout", HCaptchaError.SESSION_TIMEOUT.toString());
        assertEquals("Challenge Closed", HCaptchaError.CHALLENGE_CLOSED.toString());
        assertEquals("Rate Limited", HCaptchaError.RATE_LIMITED.toString());
        assertEquals("Unknown error", HCaptchaError.ERROR.toString());
    }

    @Test
    @SuppressWarnings("magicnumber")
    public void enum_ids() {
        assertEquals(7, HCaptchaError.NETWORK_ERROR.getErrorId());
        assertEquals(15, HCaptchaError.SESSION_TIMEOUT.getErrorId());
        assertEquals(30, HCaptchaError.CHALLENGE_CLOSED.getErrorId());
        assertEquals(31, HCaptchaError.RATE_LIMITED.getErrorId());
        assertEquals(29, HCaptchaError.ERROR.getErrorId());
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void get_enum_from_id() {
        assertEquals(HCaptchaError.NETWORK_ERROR, HCaptchaError.fromId(7));
        assertEquals(HCaptchaError.SESSION_TIMEOUT, HCaptchaError.fromId(15));
        assertEquals(HCaptchaError.CHALLENGE_CLOSED, HCaptchaError.fromId(30));
        assertEquals(HCaptchaError.RATE_LIMITED, HCaptchaError.fromId(31));
        assertEquals(HCaptchaError.ERROR, HCaptchaError.fromId(29));
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void get_enum_from_invalid_id_throws() {
        assertThrows(RuntimeException.class, () -> HCaptchaError.fromId(-999));
    }
}
