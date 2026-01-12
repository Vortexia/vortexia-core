// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.storage.metadata;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class MetadataSerializer {

  private static final Gson GSON = new GsonBuilder().create();
  private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {
  }.getType();

  public static String serialize(Map<String, Object> metadata) {
    if (metadata == null || metadata.isEmpty()) {
      return null;
    }
    return GSON.toJson(metadata);
  }

  public static Map<String, Object> deserialize(String json) {
    if (json == null || json.trim().isEmpty()) {
      return new HashMap<>();
    }
    try {
      Map<String, Object> result = GSON.fromJson(json, MAP_TYPE);
      return result != null ? result : new HashMap<>();
    } catch (Exception e) {
      return new HashMap<>();
    }
  }

  public static String getString(Map<String, Object> metadata, String key, String defaultValue) {
    Object value = metadata.get(key);
    return value instanceof String ? (String) value : defaultValue;
  }

  public static Integer getInt(Map<String, Object> metadata, String key, Integer defaultValue) {
    Object value = metadata.get(key);
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    return defaultValue;
  }

  public static Double getDouble(Map<String, Object> metadata, String key, Double defaultValue) {
    Object value = metadata.get(key);
    if (value instanceof Number) {
      return ((Number) value).doubleValue();
    }
    return defaultValue;
  }

  public static Boolean getBoolean(Map<String, Object> metadata, String key, Boolean defaultValue) {
    Object value = metadata.get(key);
    return value instanceof Boolean ? (Boolean) value : defaultValue;
  }

  @SuppressWarnings("unchecked")
  public static <T> T get(Map<String, Object> metadata, String key, Class<T> type) {
    Object value = metadata.get(key);
    if (value != null && type.isInstance(value)) {
      return (T) value;
    }
    return null;
  }
}
