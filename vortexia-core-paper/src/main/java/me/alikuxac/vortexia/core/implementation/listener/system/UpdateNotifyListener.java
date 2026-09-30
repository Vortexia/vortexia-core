// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.implementation.listener.system;

import me.alikuxac.vortexia.core.VortexiaCore;
import me.alikuxac.vortexia.core.core.permission.Permission;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class UpdateNotifyListener implements Listener {

    private final VortexiaCore plugin;

    public UpdateNotifyListener(VortexiaCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission(Permission.ADMIN.getNode())) return;

        plugin.getSchedulerService().runDelayed(() -> {
            if (plugin.getUpdateChecker() != null && plugin.getUpdateChecker().isUpdateAvailable()) {
                String message = plugin.getUpdateChecker().getUpdateMessage();
                if (message != null) {
                    for (String line : message.split("\n")) {
                        player.sendMessage(line);
                    }
                }
            }
        }, 40L);
    }
}
