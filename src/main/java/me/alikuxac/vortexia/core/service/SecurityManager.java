// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.service;

import me.alikuxac.vortexia.core.VortexiaCore;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SecurityManager {

  private final VortexiaCore plugin;
  // Set of UUIDs that ARE authenticated
  private final Set<UUID> authenticatedPlayers = new HashSet<>();
  // Map of UUIDs to their initial login location
  private final Map<UUID, Location> loginLocations = new HashMap<>();

  public SecurityManager(VortexiaCore plugin) {
    this.plugin = plugin;
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
  }
}
