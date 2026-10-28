package com.hcaptcha.sdk;

import static org.junit.Assert.assertEquals;

import org.json.JSONException;
import org.json.JSONStringer;
import org.junit.Test;

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
    public void serializes_to_json_value() throws JSONException {
        final String serialized = new JSONStringer().array().value(HCaptchaSize.INVISIBLE).endArray().toString();
        assertEquals("[\"invisible\"]", serialized);
    }

}
