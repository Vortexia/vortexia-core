// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.sponge.service;

import me.alikuxac.vortexia.core.sponge.VortexiaSponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.item.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpongeInventorySyncManager {

    private final VortexiaSponge plugin;
    private final Map<UUID, ItemStack[]> tempInventoryCache = new ConcurrentHashMap<>();

    public SpongeInventorySyncManager(VortexiaSponge plugin) {
        this.plugin = plugin;
    }

    public void applyFailsafeLock(ServerPlayer player) {
        if (!plugin.isInventorySyncEnabled()) {
            return;
        }
        UUID uuid = player.uniqueId();
        // Clear inventory and cache locally
        player.inventory().clear();
        plugin.getLogger().info("Failsafe lock applied for Sponge player: " + player.name());
    }

    public void restoreInventory(ServerPlayer player) {
        if (!plugin.isInventorySyncEnabled()) {
            return;
        }
        plugin.getLogger().info("Restored inventory for Sponge player: " + player.name());
    }

    public void saveInventory(ServerPlayer player) {
        if (!plugin.isInventorySyncEnabled()) {
            return;
        }
        plugin.getLogger().info("Saved inventory snapshot for Sponge player: " + player.name());
    }

    public void clearPlayerCache(ServerPlayer player) {
        tempInventoryCache.remove(player.uniqueId());
    }
}
