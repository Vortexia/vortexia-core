// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.backend.service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BaseSecurityService {

    private final Set<UUID> authenticatedPlayers = ConcurrentHashMap.newKeySet();

    public void markAsUnauthenticated(UUID uuid) {
        authenticatedPlayers.remove(uuid);
    }

    public void authenticate(UUID uuid) {
        authenticatedPlayers.add(uuid);
    }

    public boolean isAuthenticated(UUID uuid) {
        return authenticatedPlayers.contains(uuid);
    }

    public void clear(UUID uuid) {
        authenticatedPlayers.remove(uuid);
    }

    public void clearAll() {
        authenticatedPlayers.clear();
    }
}
