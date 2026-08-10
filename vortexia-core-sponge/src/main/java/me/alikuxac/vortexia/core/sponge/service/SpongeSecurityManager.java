// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.sponge.service;

import me.alikuxac.vortexia.core.sponge.VortexiaSponge;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpongeSecurityManager {

    private final VortexiaSponge plugin;
    private final Set<UUID> authenticatedPlayers = ConcurrentHashMap.newKeySet();

    public SpongeSecurityManager(VortexiaSponge plugin) {
        this.plugin = plugin;
    }

    public void authenticate(UUID uuid) {
        authenticatedPlayers.add(uuid);
        plugin.getLogger().info("Player authenticated on Sponge: " + uuid);
    }

    public boolean isAuthenticated(UUID uuid) {
        return authenticatedPlayers.contains(uuid);
    }

    public void clear(UUID uuid) {
        authenticatedPlayers.remove(uuid);
    }
}
