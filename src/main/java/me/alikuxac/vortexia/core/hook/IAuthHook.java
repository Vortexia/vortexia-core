// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.hook;

import org.bukkit.entity.Player;

public interface IAuthHook {

    /**
     * @return true if the auth plugin is installed and valid
     */
    boolean isInstalled();

    /**
     * @param player The player to check
     * @return true if the player has completely authenticated with the auth plugin
     */
    boolean isAuthenticated(Player player);

    /**
     * @param player The player to check
     * @return true if the auth plugin has registered this player (has an account)
     */
    boolean isRegistered(Player player);
}
