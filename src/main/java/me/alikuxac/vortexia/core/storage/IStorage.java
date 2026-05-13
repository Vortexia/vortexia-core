// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.storage;

import me.alikuxac.vortexia.api.model.Identity;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface IStorage {

  void initialize() throws StorageException;

  void shutdown();

  CompletableFuture<Void> saveIdentity(UUID uuid, UUID premiumUuid, String citizenId, String name, String pin);

  CompletableFuture<Optional<Identity>> getIdentity(UUID uuid);

  CompletableFuture<Optional<Identity>> getIdentityByName(String name);

  CompletableFuture<Optional<Identity>> getIdentityByCitizenId(String citizenId);

  CompletableFuture<Optional<Identity>> getIdentityByPremiumUuid(UUID premiumUuid);

  CompletableFuture<Boolean> updatePin(UUID uuid, String newPin);

  CompletableFuture<Boolean> updatePremiumUuid(UUID uuid, UUID premiumUuid);

  // Metadata operations
  CompletableFuture<Optional<String>> getMetadata(UUID uuid, String key);

  CompletableFuture<Void> saveMetadata(UUID uuid, String key, String value);

  CompletableFuture<Void> deleteMetadata(UUID uuid, String key);

  boolean isConnected();
}
