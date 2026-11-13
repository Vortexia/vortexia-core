// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.implementation.listener.player;

import me.alikuxac.vortexia.core.VortexiaCore;
import me.alikuxac.vortexia.core.core.storage.util.IdentityUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

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

    plugin.getSecurityManager().markAsUnauthenticated(player);
    plugin.getInventorySyncManager().applyFailsafeLock(player);

    if (plugin.getModDetectorService() != null) {
      plugin.getModDetectorService().onPlayerJoin(player);
    }

    if (plugin.getConfig().getBoolean("resource-pack.auto-send-on-join", true) && plugin.getResourcePackManager() != null) {
      plugin.getResourcePackManager().sendResourcePack(player);
    }

    plugin.getLoggerService().debug("Player joining: " + player.getName());

    plugin.getStorageManager().getIdentity(player.getUniqueId()).thenAccept(optIdentity -> {
      if (optIdentity.isPresent()) {
        plugin.getLoggerService().debug("Found existing identity for " + player.getName());
        handleIdentityAuth(player, optIdentity.get());
      } else {
        plugin.getStorageManager().getIdentityByName(player.getName()).thenAccept(optByName -> {
          if (optByName.isPresent()) {
            plugin.getLoggerService().debug("Found identity by name for " + player.getName() + " (migration needed)");
            handleIdentityAuth(player, optByName.get());
          } else {
            plugin.getLoggerService().debug("No identity found for " + player.getName() + " (new player)");
            handleNewPlayerAuth(player);
          }
        });
      }
    });
  }

  private void handleIdentityAuth(Player player, me.alikuxac.vortexia.api.model.Identity identity) {
    if (identityUtil.isOnlineMode() && identity.hasPremiumUuid()) {
      plugin.getLoggerService().debug("Premium player " + player.getName() + " detected, skipping PIN.");
      plugin.getSchedulerService().runEntity(player, () -> plugin.getSecurityManager().authenticate(player));
      return;
    }

    if (plugin.getAuthHookManager() != null && plugin.getAuthHookManager().isWaitingForLogin(player)) {
      plugin.getLoggerService().debug("External auth waiting for " + player.getName() + ", deferring PIN prompt.");
      return;
    }

    promptPinAuth(player, identity);
  }

  private void handleNewPlayerAuth(Player player) {
    if (plugin.getAuthHookManager() != null && plugin.getAuthHookManager().isWaitingForLogin(player)) {
      plugin.getLoggerService().debug("External auth waiting for new player " + player.getName() + ", deferring PIN setup.");
      return;
    }

    player.sendMessage(Component.text("Welcome to Vortexia! Please set up a security PIN to protect your account.", NamedTextColor.YELLOW));
    plugin.getSchedulerService().runDelayed(() -> {
      if (player.isOnline() && !plugin.getSecurityManager().isAuthenticated(player)) {
        plugin.getSecurityGUI().openSetup(player);
      }
    }, 10L);
  }

  public void promptPinAuth(Player player, me.alikuxac.vortexia.api.model.Identity identity) {
    if (identity.getPin() == null || identity.getPin().isEmpty()) {
      player.sendMessage(Component.text("Please set up your security PIN using /pin setup", NamedTextColor.YELLOW));
      plugin.getSchedulerService().runDelayed(() -> {
        if (player.isOnline() && !plugin.getSecurityManager().isAuthenticated(player)) {
          plugin.getSecurityGUI().openSetup(player);
        }
      }, 10L);
    } else {
      player.sendMessage(Component.text("This account is protected. Please verify your PIN using /pin verify", NamedTextColor.YELLOW));
      plugin.getSchedulerService().runDelayed(() -> {
        if (player.isOnline() && !plugin.getSecurityManager().isAuthenticated(player)) {
          plugin.getSecurityGUI().openVerify(player);
        }
      }, 10L);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    plugin.getInventorySyncManager().saveInventory(player);
    if (plugin.getResourcePackManager() != null) {
      plugin.getResourcePackManager().clearPlayerSession(player.getUniqueId());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerKick(PlayerKickEvent event) {
    Player player = event.getPlayer();
    plugin.getInventorySyncManager().saveInventory(player);
    if (plugin.getResourcePackManager() != null) {
      plugin.getResourcePackManager().clearPlayerSession(player.getUniqueId());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
    Player player = event.getPlayer();
    if (plugin.getResourcePackManager() == null) {
      return;
    }

    switch (event.getStatus()) {
      case ACCEPTED -> plugin.getLoggerService().debug("Player " + player.getName() + " accepted resource pack.");
      case SUCCESSFULLY_LOADED -> {
        plugin.getResourcePackManager().setPlayerHasResourcePack(player.getUniqueId(), true);
        plugin.getLoggerService().info("Player " + player.getName() + " successfully loaded resource pack.");
      }
      case DECLINED -> {
        boolean required = plugin.getConfig().getBoolean("resource-pack.required", plugin.getConfig().getBoolean("resource-pack.force", false));
        if (required) {
          String kickMsg = plugin.getConfig().getString("resource-pack.kick-message", "<red>You must accept the resource pack to join this server!</red>");
          player.kick(MiniMessage.miniMessage().deserialize(kickMsg));
        } else {
          plugin.getResourcePackManager().setPlayerHasResourcePack(player.getUniqueId(), false);
          plugin.getLoggerService().info("Player " + player.getName() + " declined pack. Fallback to Vanilla Map/Actionbar mode enabled.");
        }
      }
      case FAILED_DOWNLOAD -> {
        boolean required = plugin.getConfig().getBoolean("resource-pack.required", plugin.getConfig().getBoolean("resource-pack.force", false));
        if (required) {
          String kickMsg = plugin.getConfig().getString("resource-pack.kick-message", "<red>Failed to download the resource pack. Please check your network connection!</red>");
          player.kick(MiniMessage.miniMessage().deserialize(kickMsg));
        } else {
          plugin.getResourcePackManager().setPlayerHasResourcePack(player.getUniqueId(), false);
          plugin.getLoggerService().warn("Player " + player.getName() + " failed to download resource pack. Fallback to Vanilla Map/Actionbar mode enabled.");
        }
      }
      default -> {}
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onWorldSave(org.bukkit.event.world.WorldSaveEvent event) {
    long now = System.currentTimeMillis();
    if (now - lastSaveTime < 5000) {
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


}
