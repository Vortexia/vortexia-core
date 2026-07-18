// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.core.service;

import me.alikuxac.vortexia.core.VortexiaCore;
import me.alikuxac.vortexia.core.core.storage.model.InventorySnapshot;
import me.alikuxac.vortexia.core.core.storage.util.InventorySerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

public class InventorySyncManager {

  private final VortexiaCore plugin;
  // Cache temporary inventory before authentication in case server crashes or auth fails
  private final Map<UUID, ItemStack[]> tempInventoryCache = new ConcurrentHashMap<>();
  private final Map<UUID, ItemStack[]> tempEnderChestCache = new ConcurrentHashMap<>();

  public InventorySyncManager(VortexiaCore plugin) {
    this.plugin = plugin;
  }

  /**
   * Apply Failsafe Lock: Temporarily cache player's current inventory locally
   * and clear their active inventory to prevent exploiting before authentication.
   */
  public void applyFailsafeLock(Player player) {
    UUID uuid = player.getUniqueId();
    // Cache locally to prevent database failure risks
    tempInventoryCache.put(uuid, player.getInventory().getContents());
    tempEnderChestCache.put(uuid, player.getEnderChest().getContents());

    // Clear inventory
    player.getInventory().clear();
    player.getEnderChest().clear();
    plugin.getLoggerService().debug("Failsafe Lock: Cleared inventory for " + player.getName() + " and cached locally.");
  }

  /**
   * Restore player's inventory from the database (or temporary cache if database doesn't have it).
   * Includes a slight delay to resolve cross-server sync lag / race conditions.
   */
  public CompletableFuture<Void> restoreInventory(Player player) {
    UUID uuid = player.getUniqueId();
    CompletableFuture<Void> future = new CompletableFuture<>();

    // Introduce a slight delay (2 ticks / 100ms) to allow the previous server to finish saving to database
    plugin.getSchedulerService().runDelayed(() -> {
      plugin.getStorageManager().getIdentity(uuid).thenCompose(optIdentity -> {
        if (optIdentity.isEmpty() || optIdentity.get().getCitizenId() == null) {
          plugin.getSchedulerService().runEntity(player, () -> restoreFromTempCache(player));
          future.complete(null);
          return CompletableFuture.completedFuture(null);
        }

        String cid = optIdentity.get().getCitizenId();
        return plugin.getStorageManager().getStorage().getLatestInventorySnapshot(cid).thenAccept(optSnapshot -> {
          plugin.getSchedulerService().runEntity(player, () -> {
            if (optSnapshot.isPresent()) {
              try {
                InventorySnapshot snapshot = optSnapshot.get();
                ItemStack[] inv = InventorySerializer.itemStackArrayFromBase64(snapshot.getInventoryData());
                ItemStack[] ec = InventorySerializer.itemStackArrayFromBase64(snapshot.getEnderChestData());

                player.getInventory().setContents(inv);
                player.getEnderChest().setContents(ec);
                plugin.getLoggerService().info("Successfully restored inventory for " + player.getName() + " from DB.");
              } catch (Exception e) {
                player.sendMessage(Component.text("Failed to decode inventory! Restoring from temporary backup.", NamedTextColor.RED));
                plugin.getLoggerService().error("Failed to restore inventory for " + player.getName() + ": " + e.getMessage());
                restoreFromTempCache(player);
              }
            } else {
              restoreFromTempCache(player);
            }
            tempInventoryCache.remove(uuid);
            tempEnderChestCache.remove(uuid);
            future.complete(null);
          });
        });
      }).exceptionally(ex -> {
        plugin.getLoggerService().error("Error while fetching inventory from storage: " + ex.getMessage());
        plugin.getSchedulerService().runEntity(player, () -> restoreFromTempCache(player));
        future.complete(null);
        return null;
      });
    }, 2L);

    return future;
  }

  /**
   * Save player's inventory to the database as a snapshot.
   */
  public CompletableFuture<Void> saveInventory(Player player) {
    UUID uuid = player.getUniqueId();
    // Only save if the player is authenticated
    if (!plugin.getSecurityManager().isAuthenticated(player)) {
      plugin.getLoggerService().warn("Skip saving inventory for " + player.getName() + " because they are not authenticated!");
      return CompletableFuture.completedFuture(null);
    }

    ItemStack[] inv = player.getInventory().getContents();
    ItemStack[] ec = player.getEnderChest().getContents();

    return plugin.getStorageManager().getIdentity(uuid).thenCompose(optIdentity -> {
      if (optIdentity.isEmpty() || optIdentity.get().getCitizenId() == null) {
        return CompletableFuture.completedFuture(null);
      }

      String cid = optIdentity.get().getCitizenId();
      try {
        String invBase64 = InventorySerializer.itemStackArrayToBase64(inv);
        String ecBase64 = InventorySerializer.itemStackArrayToBase64(ec);

        return plugin.getStorageManager().getStorage().saveInventorySnapshot(cid, invBase64, ecBase64).thenRun(() -> {
          plugin.getLoggerService().info("Saved inventory snapshot for " + player.getName() + " (CID: " + cid + ")");
          
          // Auto-clean old snapshots: keep only the latest 10 snapshots
          int keepLimit = plugin.getConfig().getInt("inventory-sync.keep-snapshots-limit", 10);
          plugin.getStorageManager().getStorage().cleanOldInventorySnapshots(cid, keepLimit).exceptionally(err -> {
            plugin.getLoggerService().error("Failed to clean old snapshots for CID: " + cid + ": " + err.getMessage());
            return null;
          });
        });
      } catch (Exception e) {
        plugin.getLoggerService().error("Failed to serialize inventory for " + player.getName() + ": " + e.getMessage());
        return CompletableFuture.completedFuture(null);
      }
    });
  }

  private void restoreFromTempCache(Player player) {
    UUID uuid = player.getUniqueId();
    ItemStack[] inv = tempInventoryCache.remove(uuid);
    ItemStack[] ec = tempEnderChestCache.remove(uuid);

    if (inv != null) {
      player.getInventory().setContents(inv);
    }
    if (ec != null) {
      player.getEnderChest().setContents(ec);
    }
    plugin.getLoggerService().debug("Restored temporary inventory from local cache for " + player.getName());
  }

  public void clearPlayerCache(Player player) {
    tempInventoryCache.remove(player.getUniqueId());
    tempEnderChestCache.remove(player.getUniqueId());
  }
}
