// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.core.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pure per-account PIN attempt/lockout tracker (no Bukkit, no I/O).
 */
public class PinAttemptTracker {

    private final int maxAttempts;
    private final long lockoutMillis;
    private final Map<UUID, Integer> failures = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lockedUntil = new ConcurrentHashMap<>();

    public PinAttemptTracker(int maxAttempts, long lockoutSeconds) {
        this.maxAttempts = maxAttempts;
        this.lockoutMillis = lockoutSeconds * 1000L;
    }

    public boolean recordFailure(UUID uuid) {
        int count = failures.merge(uuid, 1, Integer::sum);
        if (count >= maxAttempts) {
            lockedUntil.put(uuid, System.currentTimeMillis() + lockoutMillis);
            failures.remove(uuid);
            return true;
        }
        return false;
    }

    public long lockoutSecondsLeft(UUID uuid) {
        Long until = lockedUntil.get(uuid);
        if (until == null) {
            return 0;
        }
        long left = until - System.currentTimeMillis();
        if (left <= 0) {
            lockedUntil.remove(uuid);
            return 0;
        }
        return left / 1000 + 1;
    }

    public void clear(UUID uuid) {
        failures.remove(uuid);
        lockedUntil.remove(uuid);
    }

    public void clearAll() {
        failures.clear();
        lockedUntil.clear();
    }
}