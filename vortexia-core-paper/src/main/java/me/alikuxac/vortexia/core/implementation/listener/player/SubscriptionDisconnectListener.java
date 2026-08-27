// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.implementation.listener.player;

import me.alikuxac.vortexia.core.VortexiaCore;
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
        if (plugin.getNetworkSyncManager() != null && plugin.getNetworkSyncManager().getSubscriptionManager() != null) {
            plugin.getNetworkSyncManager().getSubscriptionManager().unsubscribeAll(event.getPlayer());
        }
    }
}
