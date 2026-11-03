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
import java.util.UUID;

@RunWith(Parameterized.class)
public class HCaptchaJsonTest {
    private static final int MAX_JSON_DEPTH = 100;

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
        final JSONArray cyclicArray = new JSONArray();
        cyclicArray.put(cyclicArray);
        final JSONObject cyclicObject = new JSONObject();
        cyclicObject.put("self", cyclicObject);
        final JSONArray mixedCycle = new JSONArray();
        mixedCycle.put(Collections.singletonMap("self", mixedCycle));
        Object atDepthLimit = "leaf";
        for (int i = 0; i < MAX_JSON_DEPTH; i++) {
            atDepthLimit = new JSONArray().put(atDepthLimit);
        }
        return Arrays.asList(new Object[][] {
                {"null omitted", null, "{}"},
                {"explicit null", JSONObject.NULL, "{\"userjourney\":null}"},
                {"string", "events", "{\"userjourney\":\"events\"}"},
                {"boolean", true, "{\"userjourney\":true}"},
                {"number", 2, "{\"userjourney\":2}"},
                {"object", new JSONObject("{\"id\":\"button\"}"),
                        "{\"userjourney\":{\"id\":\"button\"}}"},
                {"array", new JSONArray("[1,null,true]"), "{\"userjourney\":[1,null,true]}"},
                {"native Java null", new JSONArray().put((Object) null), "{\"userjourney\":[null]}"},
                {"primitive array", new int[] {1, 2}, "{\"userjourney\":[1,2]}"},
                {"object array", new Object[] {"click", null}, "{\"userjourney\":[\"click\",null]}"},
                {"character array", new char[] {'a', 'b'}, "{\"userjourney\":\"ab\"}"},
                {"boxed characters", new Character[] {'a', null}, "{\"userjourney\":[\"a\",null]}"},
                {"native nested values", new JSONObject().put("events", new JSONArray().put(nested)),
                        "{\"userjourney\":{\"events\":[{\"text\":\"quote\\\"\\\\\\n\\t\u2028\u2029\","
                                + "\"empty\":null,\"values\":[true,2,[1,2]]}]}}"},
                {"nested map", nested,
                        "{\"userjourney\":{\"text\":\"quote\\\"\\\\\\n\\t\u2028\u2029\","
                                + "\"empty\":null,\"values\":[true,2,[1,2]]}}"},
                {"POJO", new Object(), null},
                {"enum", HCaptchaTheme.LIGHT, null},
                {"UUID in map", Collections.singletonMap("id", new UUID(0, 1)), null},
                {"non-string key", Collections.singletonMap(1, "value"), null},
                {"NaN", Double.NaN, null},
                {"infinity", Double.POSITIVE_INFINITY, null},
                {"nested NaN", Arrays.asList(Double.NaN), null},
                {"cycle", cyclic, null},
                {"native array cycle", cyclicArray, null},
                {"native object cycle", cyclicObject, null},
                {"mixed cycle", mixedCycle, null},
                {"native NaN", new JSONArray().put((Object) Double.NaN), null},
                {"native nested infinity", new JSONObject().put("values",
                        new JSONArray().put((Object) Double.POSITIVE_INFINITY)), null},
                {"native unsupported value", new JSONArray().put(new Object()), null},
                {"depth limit", atDepthLimit, "{\"userjourney\":" + atDepthLimit + "}"},
                {"depth exceeded", new JSONArray().put(atDepthLimit), null}
        });
    }

    @Test
    public void serializes_supported_values() throws JSONException {
        final HCaptchaVerifyParams params = HCaptchaVerifyParams.builder().userJourney(value).build();
        if (expected == null) {
            assertThrows(JSONException.class, () -> HCaptchaJson.wrap(value));
            JSONAssert.assertEquals("{}", params.toJson(), true);
        } else {
            JSONAssert.assertEquals(expected, params.toJson(), true);
        }
    }
}
