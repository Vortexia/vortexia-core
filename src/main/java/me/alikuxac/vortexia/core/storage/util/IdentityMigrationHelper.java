// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.storage.util;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.entity.Player;

import me.alikuxac.vortexia.core.VortexiaCore;
import me.alikuxac.vortexia.core.storage.IStorage;
import me.alikuxac.vortexia.api.model.Identity;

public class IdentityMigrationHelper {

  private final VortexiaCore plugin;
  private final IStorage storage;
  private final IdentityUtil identityUtil;

  public IdentityMigrationHelper(VortexiaCore plugin) {
    this.plugin = plugin;
    this.storage = plugin.getStorageManager().getStorage();
    this.identityUtil = plugin.getIdentityUtil();
  }

  public CompletableFuture<Optional<Identity>> findOrMigrateIdentity(Player player) {
    String name = player.getName();
    boolean isOnlineMode = identityUtil.isOnlineMode();

    if (isOnlineMode) {
      return findOrMigrateToOnlineMode(player, name);
    } else {
      return findOrMigrateToOfflineMode(player, name);
    }
  }

  private CompletableFuture<Optional<Identity>> findOrMigrateToOnlineMode(Player player, String name) {
    UUID premiumUuid = player.getUniqueId();

    return storage.getIdentityByPremiumUuid(premiumUuid)
        .thenCompose(optIdentity -> {
          if (optIdentity.isPresent()) {
            return CompletableFuture.completedFuture(optIdentity);
          }

          return storage.getIdentityByName(name)
              .thenCompose(optByName -> {
                if (optByName.isPresent()) {
                  Identity existing = optByName.get();

                  if (existing.getPin() != null && !existing.getPin().isEmpty()) {
                    plugin.getLoggerService().info(
                        "Migration paused for " + name + ": PIN protected. Returned for verification.");
                    return CompletableFuture.completedFuture(optByName);
                  }

                  plugin.getLoggerService().info(
                      "Migrating " + name + " to online mode (premium UUID: " + premiumUuid + ")");

                  return storage.updatePremiumUuid(existing.getUuid(), premiumUuid)
                      .thenCompose(success -> {
                        if (success) {
                          return storage.getIdentityByName(name);
                        }
                        return CompletableFuture.completedFuture(Optional.<Identity>empty());
                      });
                }

                return CompletableFuture.completedFuture(Optional.<Identity>empty());
              });
        });
  }

  private CompletableFuture<Optional<Identity>> findOrMigrateToOfflineMode(Player player, String name) {
    UUID currentUuid = player.getUniqueId();

    return storage.getIdentity(currentUuid)
        .thenCompose(optIdentity -> {
          if (optIdentity.isPresent()) {
            return CompletableFuture.completedFuture(optIdentity);
          }

          return storage.getIdentityByName(name)
              .thenCompose(optByName -> {
                if (optByName.isPresent()) {
                  Identity existing = optByName.get();
                  // For offline mode, if name exists but UUID is different, we check if it was
                  // online before
                  if (existing.hasPremiumUuid()) {
                    if (existing.getPin() != null && !existing.getPin().isEmpty()) {
                      plugin.getLoggerService().info(
                          "Migration paused for " + name + ": PIN protected. Returned for verification.");
                      return CompletableFuture.completedFuture(optByName);
                    }

                    plugin.getLoggerService().info(
                        "Migrating " + name + " to offline mode (clearing premium UUID)");
                    return storage.updatePremiumUuid(existing.getUuid(), null)
                        .thenCompose(success -> {
                          if (success) {
                            return storage.getIdentityByName(name);
                          }
                          return CompletableFuture.completedFuture(Optional.<Identity>empty());
                        });
                  }
                }
                return CompletableFuture.completedFuture(Optional.<Identity>empty());
              });
        });
  }

  public CompletableFuture<Boolean> completeMigration(Player player, Identity identity) {
    boolean isOnlineMode = identityUtil.isOnlineMode();
    UUID currentUuid = player.getUniqueId();

    if (isOnlineMode) {
      return storage.updatePremiumUuid(identity.getUuid(), currentUuid);
    } else {
      // In offline mode, if we are here, it usually means we were clearing premium
      // uuid
      return storage.updatePremiumUuid(identity.getUuid(), null);
    }
  }

  public CompletableFuture<Void> createOrUpdateIdentity(Player player, String pin) {
    UUID normalUuid = player.getUniqueId();
    UUID premiumUuid = identityUtil.getPremiumUuid(player);
    String name = player.getName();

    return storage.getIdentity(normalUuid).thenCompose(optIdentity -> {
      if (optIdentity.isPresent() && optIdentity.get().getCitizenId() != null) {
        return storage.saveIdentity(normalUuid, premiumUuid, optIdentity.get().getCitizenId(), name, pin);
      } else {
        return findUniqueCitizenId()
            .thenCompose(citizenId -> storage.saveIdentity(normalUuid, premiumUuid, citizenId, name, pin));
      }
    });
  }

  private CompletableFuture<String> findUniqueCitizenId() {
    String candidate = CitizenIdGenerator.generate();
    return storage.getIdentityByCitizenId(candidate).thenCompose(opt -> {
      if (opt.isPresent()) {
        return findUniqueCitizenId();
      } else {
        return CompletableFuture.completedFuture(candidate);
      }
    });
  }
}
