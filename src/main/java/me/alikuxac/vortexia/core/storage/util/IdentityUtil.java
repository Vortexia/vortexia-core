// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.storage.util;

import java.util.UUID;

import org.bukkit.entity.Player;

import me.alikuxac.vortexia.core.VortexiaCore;

public class IdentityUtil {

  private final VortexiaCore plugin;

  public IdentityUtil(VortexiaCore plugin) {
    this.plugin = plugin;
  }

  public boolean isOnlineMode() {
    return plugin.getServer().getOnlineMode();
  }

  public UUID getEffectiveUuid(Player player) {
    return isOnlineMode() ? player.getUniqueId() : getOfflineUuid(player);
  }

  public UUID getOfflineUuid(Player player) {
    return UUID.nameUUIDFromBytes(("OfflinePlayer:" + player.getName()).getBytes());
  }

  public UUID getPremiumUuid(Player player) {
    return isOnlineMode() ? player.getUniqueId() : null;
  }

  public UUID getNormalUuid(Player player) {
    return isOnlineMode() ? null : player.getUniqueId();
  }
}
