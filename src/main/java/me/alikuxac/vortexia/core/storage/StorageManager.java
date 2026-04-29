// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.storage;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import me.alikuxac.vortexia.core.VortexiaCore;
import me.alikuxac.vortexia.api.event.IdentityLinkEvent;
import me.alikuxac.vortexia.api.event.IdentityUpdateEvent;
import me.alikuxac.vortexia.core.storage.cache.IdentityCache;
import me.alikuxac.vortexia.core.storage.impl.MySQLStorage;
import me.alikuxac.vortexia.core.storage.impl.SQLiteStorage;
import me.alikuxac.vortexia.api.model.Identity;

public class StorageManager {

  private final VortexiaCore plugin;
  private IStorage storage;
  private IdentityCache cache;

  public StorageManager(VortexiaCore plugin) {
    this.plugin = plugin;
  }

  public void initialize() throws StorageException {
    StorageConfig config = loadConfigFromYaml();
    storage = createStorage(config);
    storage.initialize();

    cache = new IdentityCache(plugin);

    plugin.getLoggerService().info("Storage initialized: " + config.getType().getDisplayName());
  }

  private StorageConfig loadConfigFromYaml() {
    ConfigurationSection storageSection = plugin.getConfig().getConfigurationSection("storage");
    if (storageSection == null) {
      plugin.getLoggerService().warn("Storage configuration not found, using defaults (SQLite)");
      return StorageConfig.builder().build();
    }

    String typeString = storageSection.getString("type", "SQLITE");
    StorageType type;
    try {
      type = StorageType.fromString(typeString);
    } catch (IllegalArgumentException e) {
      plugin.getLoggerService().warn(e.getMessage() + ", falling back to SQLite");
      type = StorageType.SQLITE;
    }

    StorageConfig.Builder builder = StorageConfig.builder().type(type);

    if (type == StorageType.SQLITE) {
      ConfigurationSection sqliteSection = storageSection.getConfigurationSection("sqlite");
      if (sqliteSection != null) {
        builder.sqliteFile(sqliteSection.getString("file", "vortexia.db"));
      }
    } else if (type == StorageType.MYSQL) {
      ConfigurationSection mysqlSection = storageSection.getConfigurationSection("mysql");
      if (mysqlSection != null) {
        builder.mysqlHost(mysqlSection.getString("host", "localhost"))
            .mysqlPort(mysqlSection.getInt("port", 3306))
            .mysqlDatabase(mysqlSection.getString("database", "vortexia"))
            .mysqlUsername(mysqlSection.getString("username", "root"))
            .mysqlPassword(mysqlSection.getString("password", ""));

        ConfigurationSection poolSection = mysqlSection.getConfigurationSection("pool");
        if (poolSection != null) {
          builder.maxPoolSize(poolSection.getInt("maximum-pool-size", 10))
              .minIdle(poolSection.getInt("minimum-idle", 2))
              .connectionTimeout(poolSection.getLong("connection-timeout", 30000))
              .idleTimeout(poolSection.getLong("idle-timeout", 600000))
              .maxLifetime(poolSection.getLong("max-lifetime", 1800000));
        }
      }
    }

    return builder.build();
  }

  private IStorage createStorage(StorageConfig config) {
    return switch (config.getType()) {
      case SQLITE -> new SQLiteStorage(plugin, config);
      case MYSQL -> new MySQLStorage(plugin, config);
    };
  }

  public void shutdown() {
    if (cache != null) {
      cache.clear();
    }
    if (storage != null) {
      storage.shutdown();
      plugin.getLoggerService().info("Storage shutdown complete");
    }
  }

  public IStorage getStorage() {
    return storage;
  }

  public IdentityCache getCache() {
    return cache;
  }

  public CompletableFuture<Optional<Identity>> getIdentity(UUID uuid) {
    if (cache.isEnabled()) {
      Optional<Identity> cached = cache.getByUuid(uuid);
      if (cached.isPresent()) {
        return CompletableFuture.completedFuture(cached);
      }
    }

    return storage.getIdentity(uuid).thenApply(optIdentity -> {
      optIdentity.ifPresent(cache::put);
      return optIdentity;
    });
  }

  public CompletableFuture<Optional<Identity>> getIdentityByName(String name) {
    if (cache.isEnabled()) {
      Optional<Identity> cached = cache.getByName(name);
      if (cached.isPresent()) {
        return CompletableFuture.completedFuture(cached);
      }
    }

    return storage.getIdentityByName(name).thenApply(optIdentity -> {
      optIdentity.ifPresent(cache::put);
      return optIdentity;
    });
  }

  public CompletableFuture<Optional<Identity>> getIdentityByPremiumUuid(UUID premiumUuid) {
    if (cache.isEnabled()) {
      Optional<Identity> cached = cache.getByPremiumUuid(premiumUuid);
      if (cached.isPresent()) {
        return CompletableFuture.completedFuture(cached);
      }
    }

    return storage.getIdentityByPremiumUuid(premiumUuid).thenApply(optIdentity -> {
      optIdentity.ifPresent(cache::put);
      return optIdentity;
    });
  }

  public CompletableFuture<Optional<Identity>> getIdentityByCitizenId(String citizenId) {
    if (cache.isEnabled()) {
      Optional<Identity> cached = cache.getByCitizenId(citizenId);
      if (cached.isPresent()) {
        return CompletableFuture.completedFuture(cached);
      }
    }

    return storage.getIdentityByCitizenId(citizenId).thenApply(optIdentity -> {
      optIdentity.ifPresent(cache::put);
      return optIdentity;
    });
  }

  public CompletableFuture<Void> saveIdentity(UUID uuid, UUID premiumUuid, String citizenId, String name, String pin) {
    return storage.getIdentity(uuid).thenCompose(oldIdentity -> {
      return storage.saveIdentity(uuid, premiumUuid, citizenId, name, pin).thenRun(() -> {
        cache.invalidate(uuid);

        IdentityUpdateEvent.UpdateType updateType;
        if (!oldIdentity.isPresent()) {
          updateType = IdentityUpdateEvent.UpdateType.FULL_UPDATE;
        } else if (oldIdentity.get().getPremiumUuid() == null && premiumUuid != null) {
          updateType = IdentityUpdateEvent.UpdateType.PREMIUM_UUID_LINK;
        } else if (oldIdentity.get().getPremiumUuid() != null && premiumUuid == null) {
          updateType = IdentityUpdateEvent.UpdateType.PREMIUM_UUID_UNLINK;
        } else {
          updateType = IdentityUpdateEvent.UpdateType.FULL_UPDATE;
        }

        storage.getIdentity(uuid).thenAccept(newIdentity -> {
          if (newIdentity.isPresent()) {
            me.alikuxac.vortexia.api.event.IdentityUpdateEvent event = new me.alikuxac.vortexia.api.event.IdentityUpdateEvent(
                oldIdentity.orElse(null),
                newIdentity.get(),
                updateType);
            Bukkit.getScheduler().runTask(plugin, () -> {
              Bukkit.getPluginManager().callEvent(event);
            });
          }
        });
      });
    });
  }

  public CompletableFuture<Boolean> updatePremiumUuid(UUID uuid, UUID premiumUuid) {
    return storage.getIdentity(uuid).thenCompose(oldIdentity -> {
      return storage.updatePremiumUuid(uuid, premiumUuid).thenApply(success -> {
        if (success) {
          cache.invalidate(uuid);

          if (oldIdentity.isPresent()) {
            Identity old = oldIdentity.get();

            if (premiumUuid != null && old.getPremiumUuid() == null) {
              IdentityLinkEvent linkEvent = new IdentityLinkEvent(
                  uuid,
                  premiumUuid,
                  old.getName(),
                  old);
              Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.getPluginManager().callEvent(linkEvent);
              });
            }

            storage.getIdentity(uuid).thenAccept(newIdentity -> {
              if (newIdentity.isPresent()) {
                IdentityUpdateEvent.UpdateType updateType = premiumUuid != null
                    ? IdentityUpdateEvent.UpdateType.PREMIUM_UUID_LINK
                    : IdentityUpdateEvent.UpdateType.PREMIUM_UUID_UNLINK;

                IdentityUpdateEvent updateEvent = new IdentityUpdateEvent(
                    old,
                    newIdentity.get(),
                    updateType);
                Bukkit.getScheduler().runTask(plugin, () -> {
                  Bukkit.getPluginManager().callEvent(updateEvent);
                });
              }
            });
          }
        }
        return success;
      });
    });
  }

  public boolean isConnected() {
    return storage != null && storage.isConnected();
  }
}
