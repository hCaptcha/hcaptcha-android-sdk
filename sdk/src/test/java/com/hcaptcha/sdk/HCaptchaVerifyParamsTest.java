package com.hcaptcha.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class HCaptchaVerifyParamsTest {

    @Test
    public void test_verify_params_with_both_values() {
        final HCaptchaVerifyParams params = HCaptchaVerifyParams.builder()
                .phonePrefix("44")
                .phoneNumber("+44123456789")
                .build();

        assertNotNull(params);
        assertEquals("44", params.getPhonePrefix());
        assertEquals("+44123456789", params.getPhoneNumber());
    }

    @Test
    public void test_verify_params_empty() {
        final HCaptchaVerifyParams params = HCaptchaVerifyParams.builder().build();

        assertNotNull(params);
        assertNull(params.getPhonePrefix());
        assertNull(params.getPhoneNumber());
    }
}
