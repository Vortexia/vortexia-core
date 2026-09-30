// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.sponge.listener;

import me.alikuxac.vortexia.core.sponge.VortexiaSponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;

public class SpongePlayerListener {

    private final VortexiaSponge plugin;

    public SpongePlayerListener(VortexiaSponge plugin) {
        this.plugin = plugin;
    }

    @Listener
    public void onPlayerJoin(ServerSideConnectionEvent.Join event) {
        ServerPlayer player = event.player();
        plugin.getInventorySyncManager().applyFailsafeLock(player);
        plugin.getLogger().info("Player joined Sponge server: " + player.name());
    }

    @Listener
    public void onPlayerDisconnect(ServerSideConnectionEvent.Disconnect event) {
        ServerPlayer player = event.player();
        plugin.getInventorySyncManager().saveInventory(player);
        plugin.getInventorySyncManager().clearPlayerCache(player);
    }
}
