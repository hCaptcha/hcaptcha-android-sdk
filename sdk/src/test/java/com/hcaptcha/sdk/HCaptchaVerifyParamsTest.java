package com.hcaptcha.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class HCaptchaVerifyParamsTest {

    private static final String TEST_PHONE_PREFIX = "44";
    private static final String TEST_PHONE_NUMBER = "+44123456789";
    private static final String TEST_RQDATA = "test-rqdata-string";

    @Test
    public void test_verify_params_with_both_values() {
        final HCaptchaVerifyParams params = HCaptchaVerifyParams.builder()
                .phonePrefix(TEST_PHONE_PREFIX)
                .phoneNumber(TEST_PHONE_NUMBER)
                .build();

        assertNotNull(params);
        assertEquals(TEST_PHONE_PREFIX, params.getPhonePrefix());
        assertEquals(TEST_PHONE_NUMBER, params.getPhoneNumber());
    }

    @Test
    public void test_verify_params_with_rqdata() {
        final HCaptchaVerifyParams params = HCaptchaVerifyParams.builder()
                .rqdata(TEST_RQDATA)
                .build();

        assertNotNull(params);
        assertNull(params.getPhonePrefix());
        assertNull(params.getPhoneNumber());
        assertEquals(TEST_RQDATA, params.getRqdata());
    }

    @Test
    public void test_verify_params_with_all_values() {
        final HCaptchaVerifyParams params = HCaptchaVerifyParams.builder()
                .phonePrefix(TEST_PHONE_PREFIX)
                .phoneNumber(TEST_PHONE_NUMBER)
                .rqdata(TEST_RQDATA)
                .build();

        assertNotNull(params);
        assertEquals(TEST_PHONE_PREFIX, params.getPhonePrefix());
        assertEquals(TEST_PHONE_NUMBER, params.getPhoneNumber());
        assertEquals(TEST_RQDATA, params.getRqdata());
    }

    @Test
    public void test_verify_params_empty() {
        final HCaptchaVerifyParams params = HCaptchaVerifyParams.builder().build();

        assertNotNull(params);
        assertNull(params.getPhonePrefix());
        assertNull(params.getPhoneNumber());
    }
}
