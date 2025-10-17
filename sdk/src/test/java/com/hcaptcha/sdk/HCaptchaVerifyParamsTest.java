package com.hcaptcha.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
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

    @Test
    public void test_json_serialization_annotations_not_swapped() throws Exception {
        final HCaptchaVerifyParams params = HCaptchaVerifyParams.builder()
                .phonePrefix(TEST_PHONE_PREFIX)
                .phoneNumber(TEST_PHONE_NUMBER)
                .rqdata(TEST_RQDATA)
                .build();

        final ObjectMapper mapper = new ObjectMapper();
        final String json = mapper.writeValueAsString(params);

        final JSONObject jsonObject = new JSONObject(json);

        // Verify that the JSON field names match the @JsonProperty annotations
        assertNotNull(jsonObject);
        assertEquals(TEST_PHONE_PREFIX, jsonObject.getString("mfa_phoneprefix"));
        assertEquals(TEST_PHONE_NUMBER, jsonObject.getString("mfa_phone"));
        assertEquals(TEST_RQDATA, jsonObject.getString("rqdata"));

        // Verify that the original field names are NOT present in JSON
        assertEquals(false, jsonObject.has("phonePrefix"));
        assertEquals(false, jsonObject.has("phoneNumber"));
    }
}
