// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.hook;

import me.alikuxac.vortexia.core.VortexiaCore;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class AuthHookManager {

    private final VortexiaCore plugin;
    private final List<IAuthHook> hooks = new ArrayList<>();

    public AuthHookManager(VortexiaCore plugin) {
        this.plugin = plugin;
    }

    public void registerHook(IAuthHook hook) {
        if (hook.isInstalled()) {
            hooks.add(hook);
            plugin.getLoggerService().info("Registered AuthHook: " + hook.getClass().getSimpleName());
        }
    }

    /**
     * Check if player is get pending login from any auth plugin
     * 
     * @param player Player need check
     * @return true if player is get pending login, false
     */
    public boolean isWaitingForLogin(Player player) {
        if (hooks.isEmpty()) {
            return false;
        }

        for (IAuthHook hook : hooks) {
            // If there is at least one hook that the player has not authenticated 
            if (!hook.isAuthenticated(player)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAuthenticated(Player player) {
        if (hooks.isEmpty()) {
            return true; // No auth hooks, assume authenticated
        }

        for (IAuthHook hook : hooks) {
            if (!hook.isAuthenticated(player)) {
                return false;
            }
        }
        return true;
    }
}
