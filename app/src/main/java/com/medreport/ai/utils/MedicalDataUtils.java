package com.medreport.ai.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MedicalDataUtils {

    /**
     * Safely extracts a displayable string from a JsonElement.
     * Handles:
     * - "Condition" (String) -> "Condition"
     * - {"diagnosis": "Condition", ...} -> "Condition"
     * - {"name": "Condition", ...} -> "Condition"
     * - Other Objects -> JSON string
     */
    public static String getDisplayString(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return "";
        }

        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            
            // Try standard medical keys
            if (obj.has("diagnosis") && !obj.get("diagnosis").isJsonNull()) {
                return obj.get("diagnosis").getAsString();
            }
            if (obj.has("name") && !obj.get("name").isJsonNull()) {
                return obj.get("name").getAsString();
            }
            if (obj.has("test_name") && !obj.get("test_name").isJsonNull()) {
                return obj.get("test_name").getAsString();
            }
            if (obj.has("medication") && !obj.get("medication").isJsonNull()) {
                return obj.get("medication").getAsString();
            }

            // Fallback: return the first non-null string value found
            for (String key : obj.keySet()) {
                JsonElement val = obj.get(key);
                if (val.isJsonPrimitive() && val.getAsJsonPrimitive().isString()) {
                    return val.getAsString();
                }
            }

            return obj.toString();
        }

        return element.toString();
    }
}
