// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.api;

import me.alikuxac.vortexia.core.VortexiaCore;
import me.alikuxac.vortexia.core.storage.model.Identity;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class VortexiaAPI {

  private static VortexiaAPI instance;
  private final VortexiaCore plugin;

  private VortexiaAPI(VortexiaCore plugin) {
    this.plugin = plugin;
  }

  public static void initialize(VortexiaCore plugin) {
    if (instance == null) {
      instance = new VortexiaAPI(plugin);
    }
  }

  public static VortexiaAPI getInstance() {
    if (instance == null) {
      throw new IllegalStateException("VortexiaAPI not initialized");
    }
    return instance;
  }

  public Identity getIdentity(UUID uuid) {
    if (plugin.getStorageManager().getCache().isEnabled()) {
      return plugin.getStorageManager().getCache().getByUuid(uuid).orElse(null);
    }
    return null;
  }

  public Identity getIdentityByName(String name) {
    if (plugin.getStorageManager().getCache().isEnabled()) {
      return plugin.getStorageManager().getCache().getByName(name).orElse(null);
    }
    return null;
  }

  public CompletableFuture<Optional<Identity>> getIdentityAsync(UUID uuid) {
    return plugin.getStorageManager().getIdentity(uuid);
  }

  public CompletableFuture<Optional<Identity>> getIdentityByNameAsync(String name) {
    return plugin.getStorageManager().getIdentityByName(name);
  }

  public Identity getIdentityByCitizenId(String citizenId) {
    if (plugin.getStorageManager().getCache().isEnabled()) {
      return plugin.getStorageManager().getCache().getByCitizenId(citizenId).orElse(null);
    }
    return null;
  }

  public CompletableFuture<Optional<Identity>> getIdentityByCitizenIdAsync(String citizenId) {
    return plugin.getStorageManager().getIdentityByCitizenId(citizenId);
  }

  public UUID getLatestUUID(String playerName) {
    Identity identity = getIdentityByName(playerName);
    if (identity == null) {
      return null;
    }

    boolean isOnlineMode = plugin.getIdentityUtil().isOnlineMode();
    return identity.getEffectiveUuid(isOnlineMode);
  }

  public boolean isSamePerson(UUID uuid1, UUID uuid2) {
    if (uuid1.equals(uuid2)) {
      return true;
    }

    Identity identity1 = getIdentity(uuid1);
    Identity identity2 = getIdentity(uuid2);

    if (identity1 == null || identity2 == null) {
      return false;
    }

    return identity1.getName().equals(identity2.getName());
  }

  public boolean isSamePersonByUUIDs(UUID uuid1, UUID uuid2) {
    if (uuid1.equals(uuid2)) {
      return true;
    }

    Identity identity1 = getIdentity(uuid1);
    if (identity1 == null) {
      return false;
    }

    if (identity1.getUuid().equals(uuid2)) {
      return true;
    }

    if (identity1.getPremiumUuid() != null && identity1.getPremiumUuid().equals(uuid2)) {
      return true;
    }

    return false;
  }

  public UUID getEffectiveUUID(Player player) {
    return plugin.getIdentityUtil().getEffectiveUuid(player);
  }

  public boolean isOnlineMode() {
    return plugin.getIdentityUtil().isOnlineMode();
  }

  public long getCacheSize() {
    return plugin.getStorageManager().getCache().size();
  }

  public void invalidateCache(UUID uuid) {
    plugin.getStorageManager().getCache().invalidate(uuid);
  }

  public void clearCache() {
    plugin.getStorageManager().getCache().clear();
  }
}
