// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.hook.impl;

import fr.xephi.authme.api.v3.AuthMeApi;
import fr.xephi.authme.events.LoginEvent;
import fr.xephi.authme.events.RegisterEvent;
import me.alikuxac.vortexia.core.VortexiaCore;
import me.alikuxac.vortexia.core.hook.IAuthHook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class AuthMeHook implements IAuthHook, Listener {

    private final VortexiaCore plugin;
    private boolean isHooked = false;

    public AuthMeHook(VortexiaCore plugin) {
        this.plugin = plugin;
        try {
            Class.forName("fr.xephi.authme.api.v3.AuthMeApi");
            this.isHooked = true;
            Bukkit.getPluginManager().registerEvents(this, plugin);
        } catch (ClassNotFoundException e) {
            this.isHooked = false;
        }
    }

    @Override
    public boolean isInstalled() {
        return isHooked;
    }

    @Override
    public boolean isAuthenticated(Player player) {
        if (!isHooked)
            return true; // If not hooked, assume true so we don't block
        return AuthMeApi.getInstance().isAuthenticated(player);
    }

    @Override
    public boolean isRegistered(Player player) {
        if (!isHooked)
            return true;
        return AuthMeApi.getInstance().isRegistered(player.getName());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLogin(LoginEvent event) {
        // Trigger PIN logic after successful login
        handleDelayedPinPrompt(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRegister(RegisterEvent event) {
        // Trigger PIN logic after successful register
        handleDelayedPinPrompt(event.getPlayer());
    }

    private void handleDelayedPinPrompt(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            // Re-trigger the logic in PlayerListener manually now that auth is complete
            if (!plugin.getSecurityManager().isAuthenticated(player)) {
                plugin.getStorageManager().getCache().getByUuid(player.getUniqueId()).ifPresent(identity -> {
                    if (identity.getPin() == null || identity.getPin().isEmpty()) {
                        player.sendMessage(net.kyori.adventure.text.Component.text(
                                "Bạn cần cài đặt Mã PIN cấp 2 để bảo vệ tài khoản (dùng /pin setup <6-digits>).",
                                net.kyori.adventure.text.format.NamedTextColor.YELLOW));
                    } else {
                        player.sendMessage(net.kyori.adventure.text.Component.text(
                                "Tài khoản đang được bảo vệ. Vui lòng xác thực mã PIN bằng /pin verify <digits>.",
                                net.kyori.adventure.text.format.NamedTextColor.YELLOW));
                    }
                });
            }
        });
    }
}
