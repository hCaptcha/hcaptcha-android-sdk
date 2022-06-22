package com.hcaptcha.sdk;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

public class HCaptchaThemeTest {

    @Test
    public void enum_values() {
        assertEquals("dark", HCaptchaTheme.DARK.getTheme());
        assertEquals("light", HCaptchaTheme.LIGHT.getTheme());
        assertEquals("contrast", HCaptchaTheme.CONTRAST.getTheme());
    }

    @Test
    public void enum_to_string() {
        assertEquals(HCaptchaTheme.DARK.getTheme(), HCaptchaTheme.DARK.getTheme());
    }

    @Test
    public void serializes_to_json_value() throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final String serialized = objectMapper.writeValueAsString(HCaptchaTheme.CONTRAST);
        assertEquals("\"contrast\"", serialized);
    }

}
