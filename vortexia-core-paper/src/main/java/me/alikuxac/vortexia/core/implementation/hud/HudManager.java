// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.implementation.hud;

import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HudManager {

    private final Set<UUID> disabledPlayers = ConcurrentHashMap.newKeySet();

    /**
     * Checks whether the HUD rendering is enabled for the player.
     * Defaults to true unless explicitly disabled.
     */
    public boolean isHudEnabled(Player player) {
        if (player == null) return false;
        return !disabledPlayers.contains(player.getUniqueId());
    }

    /**
     * Sets the HUD enabled state for the given player.
     */
    public void setHudEnabled(Player player, boolean enabled) {
        if (player == null) return;
        if (enabled) {
            disabledPlayers.remove(player.getUniqueId());
        } else {
            disabledPlayers.add(player.getUniqueId());
        }
    }

    /**
     * Toggles the HUD enabled state for the given player.
     * @return the new state (true if enabled, false if disabled)
     */
    public boolean toggleHud(Player player) {
        if (player == null) return false;
        boolean newState = !isHudEnabled(player);
        setHudEnabled(player, newState);
        return newState;
    }

    /**
     * Cleans up player state when disconnecting.
     */
    public void removePlayer(Player player) {
        if (player != null) {
            disabledPlayers.remove(player.getUniqueId());
        }
    }

    public void removePlayer(UUID uuid) {
        if (uuid != null) {
            disabledPlayers.remove(uuid);
        }
    }

    public void clear() {
        disabledPlayers.clear();
    }
}
