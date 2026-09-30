// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.core.permission;

import me.alikuxac.vortexia.core.VortexiaCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;

import java.util.EnumMap;
import java.util.Map;

public class PermissionManager {

    private final VortexiaCore plugin;
    // Keep fully-qualified name for Bukkit Permission to avoid conflict with local Permission enum
    private final Map<Permission, org.bukkit.permissions.Permission> registeredPermissions = new EnumMap<>(Permission.class);

    public PermissionManager(VortexiaCore plugin) {
        this.plugin = plugin;
    }

    public void registerPermissions() {
        for (Permission perm : Permission.values()) {
            org.bukkit.permissions.Permission bukkitPerm = plugin.getServer().getPluginManager().getPermission(perm.getNode());
            if (bukkitPerm == null) {
                bukkitPerm = new org.bukkit.permissions.Permission(
                        perm.getNode(),
                        perm.getDescription(),
                        PermissionDefault.OP
                );
                plugin.getServer().getPluginManager().addPermission(bukkitPerm);
            }
            registeredPermissions.put(perm, bukkitPerm);
        }
        plugin.getLoggerService().info("Registered " + registeredPermissions.size() + " permissions.");
    }

    public void unregisterPermissions() {
        for (org.bukkit.permissions.Permission perm : registeredPermissions.values()) {
            plugin.getServer().getPluginManager().removePermission(perm);
        }
        registeredPermissions.clear();
    }

    public boolean hasPermission(CommandSender sender, Permission permission) {
        return sender.hasPermission(permission.getNode());
    }

    public boolean hasPermission(Player player, Permission permission) {
        return player.hasPermission(permission.getNode());
    }

    public void sendNoPermissionMessage(CommandSender sender) {
        String raw = plugin.getConfigManager().getMessage("no-permission");
        String prefix = plugin.getConfigManager().getMessage("prefix");
        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + raw));
    }

    public Component getNoPermissionComponent() {
        return Component.text("You do not have permission to perform this action.", NamedTextColor.RED);
    }

    public String getPermissionNode(Permission permission) {
        return permission.getNode();
    }

    public Map<Permission, org.bukkit.permissions.Permission> getRegisteredPermissions() {
        return registeredPermissions;
    }
}
