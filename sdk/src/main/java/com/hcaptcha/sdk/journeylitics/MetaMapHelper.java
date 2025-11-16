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
    static Map<String, Object> createMetaMap(Map.Entry<FieldKey, Object>... pairs) {
        final Map<String, Object> map = new HashMap<>();
        for (Map.Entry<FieldKey, Object> pair : pairs) {
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

