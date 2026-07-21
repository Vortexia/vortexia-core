// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.implementation.listener.player;

import me.alikuxac.vortexia.core.VortexiaCore;
import me.alikuxac.vortexia.core.core.storage.util.IdentityUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerKickEvent;

public class PlayerListener implements Listener {

  private final VortexiaCore plugin;
  private final IdentityUtil identityUtil;
  private long lastSaveTime = 0;

  public PlayerListener(VortexiaCore plugin) {
    this.plugin = plugin;
    this.identityUtil = plugin.getIdentityUtil();
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();

    // Secure by default: mark as unauthenticated immediately
    plugin.getSecurityManager().markAsUnauthenticated(player);
    // Apply Failsafe lock (clear inventory and cache locally)
    plugin.getInventorySyncManager().applyFailsafeLock(player);

    plugin.getLoggerService().debug("Player joining: " + player.getName());

    // Identity check
    plugin.getStorageManager().getIdentity(player.getUniqueId()).thenAccept(optIdentity -> {
      if (optIdentity.isPresent()) {
        plugin.getLoggerService().debug("Found existing identity for " + player.getName());
        checkAndRequestAuth(player);
      } else {
        // Try searching by name (for migrations)
        plugin.getStorageManager().getIdentityByName(player.getName()).thenAccept(optByName -> {
          if (optByName.isPresent()) {
            plugin.getLoggerService().debug("Found identity by name for " + player.getName() + " (migration needed)");
            checkAndRequestAuth(player);
          } else {
            // New player logic
            player.sendMessage(Component.text("Welcome to Vortexia! Please set up a security PIN using /pin setup <4-digit-pin>", NamedTextColor.YELLOW));
            plugin.getLoggerService().debug("No identity found for " + player.getName() + " (new player will be created on save)");
          }
        });
      }
    });
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    plugin.getInventorySyncManager().saveInventory(player);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerKick(PlayerKickEvent event) {
    Player player = event.getPlayer();
    plugin.getInventorySyncManager().saveInventory(player);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onWorldSave(org.bukkit.event.world.WorldSaveEvent event) {
    long now = System.currentTimeMillis();
    if (now - lastSaveTime < 5000) { // Debounce 5 seconds
      return;
    }
    lastSaveTime = now;

    plugin.getLoggerService().info("World save detected. Auto-saving all player inventories...");
    for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
      if (plugin.getSecurityManager().isAuthenticated(player)) {
        plugin.getInventorySyncManager().saveInventory(player);
      }
    }
  }

  private void checkAndRequestAuth(Player player) {
    plugin.getStorageManager().getCache().getByUuid(player.getUniqueId()).ifPresent(identity -> {
      // Check online mode bypass
      if (identityUtil.isOnlineMode() && identity.hasPremiumUuid()) {
        plugin.getLoggerService().debug("Premium player " + player.getName() + " detected, skipping PIN.");
        plugin.getSecurityManager().authenticate(player);
        return;
      }

      if (identity.getPin() == null || identity.getPin().isEmpty()) {
        player.sendMessage(Component.text("Please set up your security PIN using /pin setup <new_pin>", NamedTextColor.YELLOW));
      } else {
        player.sendMessage(Component.text("This account is protected. Please verify your PIN using /pin verify <your_pin>", NamedTextColor.YELLOW));
      }
    });
  }
}
