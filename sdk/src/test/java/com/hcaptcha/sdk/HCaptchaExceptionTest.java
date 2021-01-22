package com.hcaptcha.sdk;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HCaptchaExceptionTest {

    @Test
    public void exception_matches_error() {
        final HCaptchaError hCaptchaError = HCaptchaError.NETWORK_ERROR;
        try {
            throw new HCaptchaException(hCaptchaError);
        } catch (HCaptchaException hCaptchaException1) {
            assertEquals(hCaptchaError.getErrorId(), hCaptchaException1.getStatusCode());
            assertEquals(hCaptchaError.getMessage(), hCaptchaException1.getMessage());
            assertEquals(hCaptchaError, hCaptchaException1.getHCaptchaError());
        }
    }

}
