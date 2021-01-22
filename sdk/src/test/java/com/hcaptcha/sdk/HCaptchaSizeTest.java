package com.hcaptcha.sdk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HCaptchaSizeTest {

    @Test
    public void enum_values() {
        assertEquals("invisible", HCaptchaSize.INVISIBLE.getSize());
        assertEquals("compact", HCaptchaSize.COMPACT.getSize());
        assertEquals("normal", HCaptchaSize.NORMAL.getSize());
    }

    @Test
    public void enum_to_string() {
        assertEquals(HCaptchaSize.INVISIBLE.getSize(), HCaptchaSize.INVISIBLE.toString());
    }

    @Test
    public void serializes_to_json_value() throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final String serialized = objectMapper.writeValueAsString(HCaptchaSize.INVISIBLE);
        assertEquals("\"invisible\"", serialized);
    }

}
