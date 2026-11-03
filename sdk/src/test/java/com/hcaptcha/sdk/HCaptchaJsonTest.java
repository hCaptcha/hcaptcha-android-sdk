package com.hcaptcha.sdk;

import static org.junit.Assert.assertThrows;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RunWith(Parameterized.class)
public class HCaptchaJsonTest {
    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public Object value;

    @Parameterized.Parameter(2)
    public String expected;

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> values() throws JSONException {
        final Map<String, Object> nested = new HashMap<>();
        nested.put("text", "quote\"\\\n\t\u2028\u2029");
        nested.put("empty", null);
        nested.put("values", Arrays.asList(true, 2, new int[] {1, 2}));
        final Map<String, Object> cyclic = new HashMap<>();
        cyclic.put("self", cyclic);
        return Arrays.asList(new Object[][] {
                {"null omitted", null, "{}"},
                {"explicit null", JSONObject.NULL, "{\"userjourney\":null}"},
                {"string", "events", "{\"userjourney\":\"events\"}"},
                {"boolean", true, "{\"userjourney\":true}"},
                {"number", 2, "{\"userjourney\":2}"},
                {"object", new JSONObject("{\"id\":\"button\"}"),
                        "{\"userjourney\":{\"id\":\"button\"}}"},
                {"array", new JSONArray("[1,null,true]"), "{\"userjourney\":[1,null,true]}"},
                {"primitive array", new int[] {1, 2}, "{\"userjourney\":[1,2]}"},
                {"object array", new Object[] {"click", null}, "{\"userjourney\":[\"click\",null]}"},
                {"nested map", nested,
                        "{\"userjourney\":{\"text\":\"quote\\\"\\\\\\n\\t\u2028\u2029\","
                                + "\"empty\":null,\"values\":[true,2,[1,2]]}}"},
                {"POJO", new Object(), null},
                {"non-string key", Collections.singletonMap(1, "value"), null},
                {"NaN", Double.NaN, null},
                {"infinity", Double.POSITIVE_INFINITY, null},
                {"nested NaN", Arrays.asList(Double.NaN), null},
                {"cycle", cyclic, null}
        });
    }

    @Test
    public void serializes_supported_values() throws JSONException {
        final HCaptchaVerifyParams params = HCaptchaVerifyParams.builder().userJourney(value).build();
        if (expected == null) {
            assertThrows(JSONException.class, params::toJson);
        } else {
            JSONAssert.assertEquals(expected, params.toJson(), true);
        }
    }
}
