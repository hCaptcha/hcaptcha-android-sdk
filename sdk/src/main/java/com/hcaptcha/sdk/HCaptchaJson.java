package com.hcaptcha.sdk;

import android.util.Base64;

import com.hcaptcha.sdk.journeylitics.JLEvent;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Map;

/** JSON conversion for the supported user journey values, without reflective POJO serialization. */
final class HCaptchaJson {
    private static final int MAX_DEPTH = 100;

    private HCaptchaJson() {
    }

    static Object wrap(Object value) throws JSONException {
        return wrap(value, 0);
    }

    private static Object wrap(Object value, int depth) throws JSONException {
        if (depth > MAX_DEPTH) {
            throw new JSONException("User journey exceeds maximum JSON depth");
        }
        if (value == null) {
            return JSONObject.NULL;
        }
        if (value == JSONObject.NULL || value instanceof String || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Character) {
            return value.toString();
        }
        if (value instanceof Number) {
            JSONObject.numberToString((Number) value);
            return value;
        }
        if (value instanceof JLEvent) {
            return wrap(((JLEvent) value).toMap(), depth + 1);
        }
        if (value instanceof JSONObject) {
            final JSONObject source = (JSONObject) value;
            final JSONObject object = new JSONObject();
            final Iterator<String> keys = source.keys();
            while (keys.hasNext()) {
                final String key = keys.next();
                object.put(key, wrap(source.get(key), depth + 1));
            }
            return object;
        }
        if (value instanceof JSONArray) {
            final JSONArray source = (JSONArray) value;
            final JSONArray array = new JSONArray();
            for (int i = 0; i < source.length(); i++) {
                array.put(wrap(source.opt(i), depth + 1));
            }
            return array;
        }
        if (value instanceof Map) {
            final JSONObject object = new JSONObject();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (!(entry.getKey() instanceof String)) {
                    throw new JSONException("User journey map keys must be strings");
                }
                object.put((String) entry.getKey(), wrap(entry.getValue(), depth + 1));
            }
            return object;
        }
        if (value instanceof Iterable) {
            final JSONArray array = new JSONArray();
            for (Object item : (Iterable<?>) value) {
                array.put(wrap(item, depth + 1));
            }
            return array;
        }
        if (value instanceof byte[]) {
            return Base64.encodeToString((byte[]) value, Base64.NO_WRAP);
        }
        if (value instanceof char[]) {
            return new String((char[]) value);
        }
        if (value.getClass().isArray()) {
            final JSONArray array = new JSONArray();
            for (int i = 0; i < Array.getLength(value); i++) {
                array.put(wrap(Array.get(value, i), depth + 1));
            }
            return array;
        }
        throw new JSONException("Unsupported user journey type: " + value.getClass().getName());
    }
}
