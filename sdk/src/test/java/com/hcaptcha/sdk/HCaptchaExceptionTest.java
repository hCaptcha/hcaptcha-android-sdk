package com.hcaptcha.sdk;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class HCaptchaExceptionTest {

    @Test
    public void exception_matches_error() {
        final HCaptchaError error = HCaptchaError.NETWORK_ERROR;
        try {
            throw new HCaptchaException(error);
        } catch (HCaptchaException hCaptchaException1) {
            assertEquals(error.getErrorId(), hCaptchaException1.getStatusCode());
            assertEquals(error.getMessage(), hCaptchaException1.getMessage());
            assertEquals(error, hCaptchaException1.gethCaptchaError());
        }
    }
}
