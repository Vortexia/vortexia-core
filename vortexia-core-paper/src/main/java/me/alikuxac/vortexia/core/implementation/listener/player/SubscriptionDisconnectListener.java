// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.implementation.listener.player;

import me.alikuxac.vortexia.core.VortexiaCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Clean up active subscriptions when player disconnects to prevent memory leaks.
 */
public class SubscriptionDisconnectListener implements Listener {

    private final VortexiaCore plugin;

    public SubscriptionDisconnectListener(VortexiaCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.getHudManager() != null) {
            plugin.getHudManager().removePlayer(player);
        }
        if (plugin.getModDetectorService() != null) {
            plugin.getModDetectorService().onPlayerQuit(player);
        }
        if (plugin.getNetworkSyncManager() != null && plugin.getNetworkSyncManager().getSubscriptionManager() != null) {
            plugin.getNetworkSyncManager().getSubscriptionManager().unsubscribeAll(player);
        }
    }
}
