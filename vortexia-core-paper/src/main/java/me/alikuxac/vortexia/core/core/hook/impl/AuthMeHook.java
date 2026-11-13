// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.core.hook.impl;

import fr.xephi.authme.api.v3.AuthMeApi;
import fr.xephi.authme.events.LoginEvent;
import fr.xephi.authme.events.LogoutEvent;
import me.alikuxac.vortexia.core.VortexiaCore;
import me.alikuxac.vortexia.core.core.hook.IAuthHook;
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
        
        if (!plugin.getSecurityManager().isAuthenticated(player)) {
            plugin.getStorageManager().getIdentity(player.getUniqueId()).thenAccept(optIdentity -> {
                if (optIdentity.isPresent()) {
                    promptPin(player, optIdentity.get());
                } else {
                    plugin.getStorageManager().getIdentityByName(player.getName()).thenAccept(optByName -> {
                        if (optByName.isPresent()) {
                            promptPin(player, optByName.get());
                        } else {
                            player.sendMessage(Component.text("AuthMe login successful! Please set up your security PIN.", NamedTextColor.YELLOW));
                            plugin.getSchedulerService().runDelayed(() -> {
                                if (player.isOnline() && !plugin.getSecurityManager().isAuthenticated(player)) {
                                    plugin.getSecurityGUI().openSetup(player);
                                }
                            }, 5L);
                        }
                    });
                }
            });
        }
    }

    private void promptPin(Player player, me.alikuxac.vortexia.api.model.Identity identity) {
        if (identity.getPin() == null || identity.getPin().isEmpty()) {
            player.sendMessage(Component.text("AuthMe login successful! Now please set up your security PIN.", NamedTextColor.YELLOW));
            plugin.getSchedulerService().runDelayed(() -> {
                if (player.isOnline() && !plugin.getSecurityManager().isAuthenticated(player)) {
                    plugin.getSecurityGUI().openSetup(player);
                }
            }, 5L);
        } else {
            player.sendMessage(Component.text("AuthMe login successful! Now please verify your PIN.", NamedTextColor.YELLOW));
            plugin.getSchedulerService().runDelayed(() -> {
                if (player.isOnline() && !plugin.getSecurityManager().isAuthenticated(player)) {
                    plugin.getSecurityGUI().openVerify(player);
                }
            }, 5L);
        }
    }

    @EventHandler
    public void onAuthMeLogout(LogoutEvent event) {
        plugin.getSecurityManager().clear(event.getPlayer());
    }
}
