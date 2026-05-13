// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.listener;

import me.alikuxac.vortexia.core.VortexiaCore;
import me.alikuxac.vortexia.core.storage.util.IdentityUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerListener implements Listener {

  private final VortexiaCore plugin;
  private final IdentityUtil identityUtil;

  public PlayerListener(VortexiaCore plugin) {
    this.plugin = plugin;
    this.identityUtil = plugin.getIdentityUtil();
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();

    // Secure by default: mark as unauthenticated immediately
    plugin.getSecurityManager().markAsUnauthenticated(player);

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
