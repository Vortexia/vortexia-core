// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.storage.model;

import java.util.UUID;

public class Identity {

  private final String citizenId;
  private final UUID uuid;
  private final UUID premiumUuid;
  private final String name;
  private final String pin;
  private final long createdAt;
  private final long updatedAt;

  private Identity(Builder builder) {
    this.citizenId = builder.citizenId;
    this.uuid = builder.uuid;
    this.premiumUuid = builder.premiumUuid;
    this.name = builder.name;
    this.pin = builder.pin;
    this.createdAt = builder.createdAt;
    this.updatedAt = builder.updatedAt;
  }

  public String getCitizenId() {
    return citizenId;
  }

  public UUID getUuid() {
    return uuid;
  }

  public UUID getPremiumUuid() {
    return premiumUuid;
  }

  public String getName() {
    return name;
  }

  public String getPin() {
    return pin;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public UUID getEffectiveUuid(boolean isOnlineMode) {
    return isOnlineMode ? (premiumUuid != null ? premiumUuid : uuid) : uuid;
  }

  public boolean hasPremiumUuid() {
    return premiumUuid != null;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String citizenId;
    private UUID uuid;
    private UUID premiumUuid;
    private String name;
    private String pin;
    private long createdAt;
    private long updatedAt;

    public Builder citizenId(String citizenId) {
      this.citizenId = citizenId;
      return this;
    }

    public Builder uuid(UUID uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder premiumUuid(UUID premiumUuid) {
      this.premiumUuid = premiumUuid;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder pin(String pin) {
      this.pin = pin;
      return this;
    }

    public Builder createdAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder updatedAt(long updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }

    public Identity build() {
      if (uuid == null) {
        throw new IllegalStateException("UUID cannot be null");
      }
      if (name == null || name.trim().isEmpty()) {
        throw new IllegalStateException("Name cannot be null or empty");
      }
      return new Identity(this);
    }
  }
}
