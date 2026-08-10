// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.proxy;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyAuthManager {

    private static final String CHANNEL = "vortexia:main";
    private final Set<UUID> authenticatedPlayers = ConcurrentHashMap.newKeySet();

    public static String getChannel() {
        return CHANNEL;
    }

    public void authenticate(UUID uuid) {
        authenticatedPlayers.add(uuid);
    }

    public void unauthenticate(UUID uuid) {
        authenticatedPlayers.remove(uuid);
    }

    public boolean isAuthenticated(UUID uuid) {
        return authenticatedPlayers.contains(uuid);
    }

    public void clear() {
        authenticatedPlayers.clear();
    }
}
