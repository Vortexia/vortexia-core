// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.core.storage.model;

public class InventorySnapshot {

  private final String citizenId;
  private final String inventoryData;
  private final String enderChestData;
  private final long timestamp;

  public InventorySnapshot(String citizenId, String inventoryData, String enderChestData, long timestamp) {
    this.citizenId = citizenId;
    this.inventoryData = inventoryData;
    this.enderChestData = enderChestData;
    this.timestamp = timestamp;
  }

  public String getCitizenId() {
    return citizenId;
  }

  public String getInventoryData() {
    return inventoryData;
  }

  public String getEnderChestData() {
    return enderChestData;
  }

  public long getTimestamp() {
    return timestamp;
  }
}
