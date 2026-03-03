// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.service;

import me.alikuxac.vortexia.core.VortexiaCore;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SecurityManager {

  private final VortexiaCore plugin;
  // Set of UUIDs that are NOT yet authenticated
  private final Set<UUID> unauthenticatedPlayers = new HashSet<>();

  public SecurityManager(VortexiaCore plugin) {
    this.plugin = plugin;
  }

  public void markAsUnauthenticated(Player player) {
    unauthenticatedPlayers.add(player.getUniqueId());
    plugin.getLoggerService().debug("Player " + player.getName() + " marked as UNAUTHENTICATED");
  }

  public void authenticate(Player player) {
    unauthenticatedPlayers.remove(player.getUniqueId());
    plugin.getLoggerService().info("Player " + player.getName() + " successfully AUTHENTICATED");
  }

  public boolean isAuthenticated(Player player) {
    return !unauthenticatedPlayers.contains(player.getUniqueId());
  }

  public void clear(Player player) {
    unauthenticatedPlayers.remove(player.getUniqueId());
  }

  public void clearAll() {
    unauthenticatedPlayers.clear();
  }
}
