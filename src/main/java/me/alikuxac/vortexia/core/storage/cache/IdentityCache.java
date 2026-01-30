// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.storage.cache;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import me.alikuxac.vortexia.core.VortexiaCore;
import me.alikuxac.vortexia.core.storage.model.Identity;

public class IdentityCache {

  private final VortexiaCore plugin;
  private final Cache<UUID, Identity> uuidCache;
  private final Cache<String, Identity> nameCache;
  private final Cache<String, Identity> citizenIdCache;
  private final Cache<UUID, Identity> premiumUuidCache;
  private final Cache<UUID, ReadWriteLock> locks;
  private final boolean enabled;

  public IdentityCache(VortexiaCore plugin) {
    this.plugin = plugin;
    this.enabled = plugin.getConfig().getBoolean("cache.enabled", true);

    if (!enabled) {
      this.uuidCache = null;
      this.nameCache = null;
      this.citizenIdCache = null;
      this.premiumUuidCache = null;
      this.locks = null;
      plugin.getLoggerService().info("Identity cache is disabled");
      return;
    }

    int maxSize = plugin.getConfig().getInt("cache.max-size", 10000);
    int ttlMinutes = plugin.getConfig().getInt("cache.ttl-minutes", 30);

    Caffeine<Object, Object> builder = Caffeine.newBuilder()
        .maximumSize(maxSize);

    if (ttlMinutes > 0) {
      builder.expireAfterWrite(ttlMinutes, TimeUnit.MINUTES);
    }

    this.uuidCache = builder.build();
    this.nameCache = builder.build();
    this.citizenIdCache = builder.build();
    this.premiumUuidCache = builder.build();
    this.locks = Caffeine.newBuilder()
        .maximumSize(maxSize)
        .expireAfterAccess(ttlMinutes > 0 ? ttlMinutes : 60, TimeUnit.MINUTES)
        .build();

    plugin.getLoggerService().info("Identity cache initialized (max: " + maxSize + ", TTL: " + ttlMinutes + "m)");
  }

  public void put(Identity identity) {
    if (!enabled || identity == null)
      return;

    uuidCache.put(identity.getUuid(), identity);
    nameCache.put(identity.getName(), identity);
    if (identity.getCitizenId() != null) {
      citizenIdCache.put(identity.getCitizenId(), identity);
    }

    if (identity.getPremiumUuid() != null) {
      premiumUuidCache.put(identity.getPremiumUuid(), identity);
    }
  }

  public Optional<Identity> getByUuid(UUID uuid) {
    if (!enabled || uuid == null)
      return Optional.empty();
    return Optional.ofNullable(uuidCache.getIfPresent(uuid));
  }

  public Optional<Identity> getByName(String name) {
    if (!enabled || name == null)
      return Optional.empty();
    return Optional.ofNullable(nameCache.getIfPresent(name));
  }

  public Optional<Identity> getByCitizenId(String citizenId) {
    if (!enabled || citizenId == null)
      return Optional.empty();
    return Optional.ofNullable(citizenIdCache.getIfPresent(citizenId));
  }

  public Optional<Identity> getByPremiumUuid(UUID premiumUuid) {
    if (!enabled || premiumUuid == null)
      return Optional.empty();
    return Optional.ofNullable(premiumUuidCache.getIfPresent(premiumUuid));
  }

  public void invalidate(UUID uuid) {
    if (!enabled || uuid == null)
      return;

    Identity identity = uuidCache.getIfPresent(uuid);
    if (identity != null) {
      uuidCache.invalidate(uuid);
      nameCache.invalidate(identity.getName());
      if (identity.getCitizenId() != null) {
        citizenIdCache.invalidate(identity.getCitizenId());
      }
      if (identity.getPremiumUuid() != null) {
        premiumUuidCache.invalidate(identity.getPremiumUuid());
      }
      locks.invalidate(uuid);
    }
  }

  public void invalidateByName(String name) {
    if (!enabled || name == null)
      return;

    Identity identity = nameCache.getIfPresent(name);
    if (identity != null) {
      invalidate(identity.getUuid());
    }
  }

  public void clear() {
    if (!enabled)
      return;

    uuidCache.invalidateAll();
    nameCache.invalidateAll();
    citizenIdCache.invalidateAll();
    premiumUuidCache.invalidateAll();
    locks.invalidateAll();
  }

  public long size() {
    if (!enabled)
      return 0;
    return uuidCache.estimatedSize();
  }

  public boolean isEnabled() {
    return enabled;
  }

  public <T> T withReadLock(UUID uuid, Function<Identity, T> operation) {
    if (!enabled) {
      Identity identity = uuidCache.getIfPresent(uuid);
      return identity != null ? operation.apply(identity) : null;
    }

    ReadWriteLock lock = locks.get(uuid, k -> new ReentrantReadWriteLock());
    lock.readLock().lock();
    try {
      Identity identity = uuidCache.getIfPresent(uuid);
      return identity != null ? operation.apply(identity) : null;
    } finally {
      lock.readLock().unlock();
    }
  }

  public void withWriteLock(UUID uuid, Consumer<Identity> operation) {
    if (!enabled) {
      Identity identity = uuidCache.getIfPresent(uuid);
      if (identity != null) {
        operation.accept(identity);
      }
      return;
    }

    ReadWriteLock lock = locks.get(uuid, k -> new ReentrantReadWriteLock());
    lock.writeLock().lock();
    try {
      Identity identity = uuidCache.getIfPresent(uuid);
      if (identity != null) {
        operation.accept(identity);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }
}
