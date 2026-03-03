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
     * Kiem tra xem nguoi choi co dang bi pending login tu bat ky auth plugin nao
     * khong.
     * 
     * @param player Nguoi choi can kiem tra
     * @return true neu nguoi choi CHUA login xong (dang cho login/register), false
     *         neu khong co plugin auth nao hoac da login xong.
     */
    public boolean isWaitingForLogin(Player player) {
        if (hooks.isEmpty()) {
            return false;
        }

        for (IAuthHook hook : hooks) {
            // Neu co it nhat 1 hook ma player chua authenticate -> dang cho login
            if (!hook.isAuthenticated(player)) {
                return true;
            }
        }
        return false;
    }
}
