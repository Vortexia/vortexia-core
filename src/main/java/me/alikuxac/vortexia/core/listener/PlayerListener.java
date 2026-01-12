// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.listener;

import me.alikuxac.vortexia.core.VortexiaCore;
import me.alikuxac.vortexia.core.event.IdentityLoadEvent;
import me.alikuxac.vortexia.core.storage.model.Identity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Optional;

public class PlayerListener implements Listener {

  private final VortexiaCore plugin;

  public PlayerListener(VortexiaCore plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();

    if (!plugin.getStorageManager().getCache().isEnabled()) {
      return;
    }

    boolean asyncLoading = plugin.getConfig().getBoolean("cache.async-loading", true);

    if (asyncLoading) {
      loadIdentityAsync(player);
    } else {
      loadIdentitySync(player);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerQuit(PlayerQuitEvent event) {
    if (!plugin.getStorageManager().getCache().isEnabled()) {
      return;
    }

    boolean invalidateOnQuit = plugin.getConfig().getBoolean("cache.invalidate-on-quit", false);

    if (invalidateOnQuit) {
      plugin.getStorageManager().getCache().invalidate(event.getPlayer().getUniqueId());
    }
  }

  private void loadIdentityAsync(Player player) {
    plugin.getStorageManager().getIdentity(player.getUniqueId())
        .thenAccept(optIdentity -> {
          if (optIdentity.isPresent()) {
            Identity identity = optIdentity.get();

            IdentityLoadEvent loadEvent = new IdentityLoadEvent(
                identity,
                IdentityLoadEvent.LoadSource.DATABASE);
            Bukkit.getScheduler().runTask(plugin, () -> {
              Bukkit.getPluginManager().callEvent(loadEvent);
            });

            plugin.getLoggerService().debug(
                "Loaded identity for " + player.getName() + " into cache");
          } else {
            plugin.getLoggerService().debug(
                "No identity found for " + player.getName() + " (new player)");
          }
        })
        .exceptionally(throwable -> {
          plugin.getLoggerService().warn(
              "Failed to load identity for " + player.getName() + ": " + throwable.getMessage());
          return null;
        });
  }

  private void loadIdentitySync(Player player) {
    Optional<Identity> cached = plugin.getStorageManager().getCache().getByUuid(player.getUniqueId());

    if (cached.isPresent()) {
      IdentityLoadEvent loadEvent = new IdentityLoadEvent(
          cached.get(),
          IdentityLoadEvent.LoadSource.CACHE);
      Bukkit.getPluginManager().callEvent(loadEvent);
    }
  }
}
