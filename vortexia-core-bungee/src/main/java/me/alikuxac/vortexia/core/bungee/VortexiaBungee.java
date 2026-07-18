// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.bungee;

import net.md_5.bungee.api.plugin.Plugin;

public class VortexiaBungee extends Plugin {
    @Override
    public void onEnable() {
        getLogger().info("VortexiaCore proxy support (BungeeCord/Waterfall) enabled successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("VortexiaCore proxy support (BungeeCord/Waterfall) disabled.");
    }
}
