package com.hcaptcha.sdk;

import static org.junit.Assert.assertEquals;

import org.json.JSONException;
import org.json.JSONStringer;
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
    public void serializes_to_json_value() throws JSONException {
        final String serialized = new JSONStringer().array().value(HCaptchaTheme.CONTRAST).endArray().toString();
        assertEquals("[\"contrast\"]", serialized);
    }

}
