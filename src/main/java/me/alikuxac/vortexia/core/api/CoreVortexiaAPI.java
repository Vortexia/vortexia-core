// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.api;

import me.alikuxac.vortexia.api.VortexiaAPI;
import me.alikuxac.vortexia.api.addon.AddonManager;
import me.alikuxac.vortexia.api.model.Identity;
import me.alikuxac.vortexia.core.VortexiaCore;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CoreVortexiaAPI implements VortexiaAPI {

    private final VortexiaCore plugin;

    public CoreVortexiaAPI(VortexiaCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public AddonManager getAddonManager() {
        return plugin.getAddonManager();
    }

    @Override
    public Identity getIdentity(UUID uuid) {
        if (plugin.getStorageManager().getCache().isEnabled()) {
            return plugin.getStorageManager().getCache().getByUuid(uuid).orElse(null);
        }
        return null;
    }

    @Override
    public Identity getIdentityByName(String name) {
        if (plugin.getStorageManager().getCache().isEnabled()) {
            return plugin.getStorageManager().getCache().getByName(name).orElse(null);
        }
        return null;
    }

    @Override
    public CompletableFuture<Optional<Identity>> getIdentityAsync(UUID uuid) {
        return plugin.getStorageManager().getIdentity(uuid);
    }

    @Override
    public CompletableFuture<Optional<Identity>> getIdentityByNameAsync(String name) {
        return plugin.getStorageManager().getIdentityByName(name);
    }

    @Override
    public Identity getIdentityByCitizenId(String citizenId) {
        if (plugin.getStorageManager().getCache().isEnabled()) {
            return plugin.getStorageManager().getCache().getByCitizenId(citizenId).orElse(null);
        }
        return null;
    }

    @Override
    public CompletableFuture<Optional<Identity>> getIdentityByCitizenIdAsync(String citizenId) {
        return plugin.getStorageManager().getIdentityByCitizenId(citizenId);
    }

    @Override
    public UUID getLatestUUID(String playerName) {
        Identity identity = getIdentityByName(playerName);
        if (identity == null) {
            return null;
        }

        boolean isOnlineMode = plugin.getIdentityUtil().isOnlineMode();
        return identity.getEffectiveUuid(isOnlineMode);
    }

    @Override
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

    @Override
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

    @Override
    public UUID getEffectiveUUID(Player player) {
        return plugin.getIdentityUtil().getEffectiveUuid(player);
    }

    @Override
    public boolean isOnlineMode() {
        return plugin.getIdentityUtil().isOnlineMode();
    }

    @Override
    public long getCacheSize() {
        return plugin.getStorageManager().getCache().size();
    }

    @Override
    public void invalidateCache(UUID uuid) {
        plugin.getStorageManager().getCache().invalidate(uuid);
    }

    @Override
    public void clearCache() {
        plugin.getStorageManager().getCache().clear();
    }

    @Override
    public CompletableFuture<Optional<String>> getMetadata(UUID uuid, String key) {
        return plugin.getStorageManager().getMetadata(uuid, key);
    }

    @Override
    public CompletableFuture<Void> setMetadata(UUID uuid, String key, String value) {
        return plugin.getStorageManager().setMetadata(uuid, key, value);
    }

    @Override
    public CompletableFuture<Void> removeMetadata(UUID uuid, String key) {
        return plugin.getStorageManager().removeMetadata(uuid, key);
    }
}
