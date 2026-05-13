// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.hook.impl;

import fr.xephi.authme.api.v3.AuthMeApi;
import fr.xephi.authme.events.LoginEvent;
import fr.xephi.authme.events.LogoutEvent;
import me.alikuxac.vortexia.core.VortexiaCore;
import me.alikuxac.vortexia.core.hook.IAuthHook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class AuthMeHook implements IAuthHook, Listener {

    private final VortexiaCore plugin;
    private final AuthMeApi authMeApi;

    public AuthMeHook(VortexiaCore plugin) {
        this.plugin = plugin;
        this.authMeApi = AuthMeApi.getInstance();
    }

    @Override
    public boolean isInstalled() {
        return Bukkit.getPluginManager().getPlugin("AuthMe") != null;
    }

    @Override
    public boolean isAuthenticated(Player player) {
        return authMeApi.isAuthenticated(player);
    }

    @Override
    public boolean isRegistered(Player player) {
        return authMeApi.isRegistered(player.getName());
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onAuthMeLogin(LoginEvent event) {
        Player player = event.getPlayer();
        plugin.getLoggerService().debug("AuthMe login detected for " + player.getName());
        
        // After AuthMe login, we might still need PIN verification
        if (!plugin.getSecurityManager().isAuthenticated(player)) {
            plugin.getStorageManager().getCache().getByUuid(player.getUniqueId()).ifPresent(identity -> {
                if (identity.getPin() == null || identity.getPin().isEmpty()) {
                    player.sendMessage(Component.text("AuthMe login successful! Now please set up your security PIN using /pin setup <new_pin>", NamedTextColor.YELLOW));
                } else {
                    player.sendMessage(Component.text("AuthMe login successful! Now please verify your PIN using /pin verify <your_pin>", NamedTextColor.YELLOW));
                }
            });
        }
    }

    @EventHandler
    public void onAuthMeLogout(LogoutEvent event) {
        plugin.getSecurityManager().clear(event.getPlayer());
    }
}
