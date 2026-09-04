// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.core.service;

import me.alikuxac.vortexia.core.VortexiaCore;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks modded vs vanilla clients based on plugin messaging channel handshakes.
 */
public class ModDetectorService {

    private final VortexiaCore plugin;
    private final Map<UUID, Boolean> moddedPlayers = new ConcurrentHashMap<>();

    public ModDetectorService(VortexiaCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Marks a player as having the client mod installed.
     */
    public void markAsModded(Player player) {
        if (player == null) return;
        Boolean previous = moddedPlayers.put(player.getUniqueId(), true);
        if (previous == null || !previous) {
            plugin.getLoggerService().info("⚡ Player " + player.getName() + " connected using [Vortexia HUD Mod]!");
        }
    }

    /**
     * Checks if a player has the client mod installed.
     */
    public boolean isModded(Player player) {
        if (player == null) return false;
        return moddedPlayers.getOrDefault(player.getUniqueId(), false);
    }

    /**
     * Called when a player joins the server.
     */
    public void onPlayerJoin(Player player) {
        moddedPlayers.put(player.getUniqueId(), false);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !isModded(player)) {
                plugin.getLoggerService().info("ℹ️ Player " + player.getName() + " connected as [Vanilla Client] (Using Vanilla HUD Fallback).");
            }
        }, 40L); // Log after 2 seconds if no mod handshake received
    }

    /**
     * Clean up player on disconnect.
     */
    public void onPlayerQuit(Player player) {
        if (player != null) {
            moddedPlayers.remove(player.getUniqueId());
        }
    }

    public void clear() {
        moddedPlayers.clear();
    }
}
