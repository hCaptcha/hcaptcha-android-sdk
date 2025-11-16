package com.hcaptcha.sdk.journeylitics;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to create meta field mappings (O(1) - no iteration)
 */
@SuppressWarnings("PMD.UseUtilityClass")
final class MetaMapHelper {
    private MetaMapHelper() {
        // Utility class - prevent instantiation
    }

    @SafeVarargs
    static Map<String, String> createMetaMap(Map.Entry<FieldKey, String>... pairs) {
        final Map<String, String> map = new HashMap<>();
        for (Map.Entry<FieldKey, String> pair : pairs) {
            map.put(pair.getKey().getJsonKey(), pair.getValue());
        }
        return map;
    }

    @SafeVarargs
    static Map<String, Object> createFieldMap(Map.Entry<String, Object>... pairs) {
        final Map<String, Object> map = new HashMap<>();
        for (Map.Entry<String, Object> pair : pairs) {
            map.put(pair.getKey(), pair.getValue());
        }
        return map;
    }
}

