// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.storage;

public enum StorageType {
  SQLITE("SQLite"),
  MYSQL("MySQL");

  private final String displayName;

  StorageType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public static StorageType fromString(String type) {
    if (type == null || type.trim().isEmpty()) {
      throw new IllegalArgumentException("Storage type cannot be null or empty");
    }

    String normalized = type.trim().toUpperCase();
    try {
      return StorageType.valueOf(normalized);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Invalid storage type: " + type + ". Valid types: SQLITE, MYSQL");
    }
  }
}
