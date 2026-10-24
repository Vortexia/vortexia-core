// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.core.service;

import me.alikuxac.vortexia.core.VortexiaCore;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SecurityManager {

  private final VortexiaCore plugin;
  // Set of UUIDs that ARE authenticated
  private final Set<UUID> authenticatedPlayers = new HashSet<>();
  // Map of UUIDs to their initial login location
  private final Map<UUID, Location> loginLocations = new HashMap<>();
  // ponytail: PIN lockout is in-memory; restart resets attempts (acceptable, PIN attempts are per-session)
  private final Map<UUID, Integer> pinFailures = new ConcurrentHashMap<>();
  private final Map<UUID, Long> pinLockedUntil = new ConcurrentHashMap<>();

  public SecurityManager(VortexiaCore plugin) {
    this.plugin = plugin;
  }

  /**
   * Records a failed PIN attempt. Returns true if the player is now locked out.
   */
  public boolean recordPinFailure(Player player) {
    int maxAttempts = plugin.getConfig().getInt("security.pin.max-attempts", 3);
    int failures = pinFailures.merge(player.getUniqueId(), 1, Integer::sum);
    if (failures >= maxAttempts) {
      int lockoutSeconds = plugin.getConfig().getInt("security.pin.lockout-seconds", 300);
      pinLockedUntil.put(player.getUniqueId(), System.currentTimeMillis() + lockoutSeconds * 1000L);
      pinFailures.remove(player.getUniqueId());
      return true;
    }
    return false;
  }

  /**
   * Remaining lockout seconds, or 0 if not locked.
   */
  public long pinLockoutSecondsLeft(Player player) {
    Long until = pinLockedUntil.get(player.getUniqueId());
    if (until == null) return 0;
    long left = until - System.currentTimeMillis();
    if (left <= 0) {
      pinLockedUntil.remove(player.getUniqueId());
      return 0;
    }
    return left / 1000 + 1;
  }

  public void clearPinFailures(Player player) {
    pinFailures.remove(player.getUniqueId());
    pinLockedUntil.remove(player.getUniqueId());
  }

  public void markAsUnauthenticated(Player player) {
    authenticatedPlayers.remove(player.getUniqueId());
    loginLocations.put(player.getUniqueId(), player.getLocation());
    plugin.getLoggerService().debug("Player " + player.getName() + " marked as UNAUTHENTICATED at " + player.getLocation());
  }

  public void authenticate(Player player) {
    authenticatedPlayers.add(player.getUniqueId());
    plugin.getLoggerService().info("Player " + player.getName() + " successfully AUTHENTICATED");
    
    // Teleport back to login location if stored
    Location loginLoc = loginLocations.remove(player.getUniqueId());
    if (loginLoc != null) {
        player.teleport(loginLoc);
        plugin.getLoggerService().debug("Player " + player.getName() + " teleported back to login location.");
    }

    // Restore inventory from database or temporary cache
    if (plugin.getInventorySyncManager() != null) {
      plugin.getInventorySyncManager().restoreInventory(player);
    }

    // Broadcast to other servers in the proxy
    if (plugin.getProxySyncService() != null) {
      plugin.getProxySyncService().broadcastAuthentication(player);
    }
  }

  public void authenticateLocally(UUID uuid) {
    authenticatedPlayers.add(uuid);
    loginLocations.remove(uuid); // Clear if exists
    plugin.getLoggerService().debug("UUID " + uuid + " successfully AUTHENTICATED LOCALLY via Sync");
  }

  public boolean isAuthenticated(Player player) {
    return authenticatedPlayers.contains(player.getUniqueId());
  }

  public void clear(Player player) {
    authenticatedPlayers.remove(player.getUniqueId());
    loginLocations.remove(player.getUniqueId());
  }

  public void clearAll() {
    authenticatedPlayers.clear();
    loginLocations.clear();
    pinFailures.clear();
    pinLockedUntil.clear();
  }
}
