package com.vortexia.core;

import org.bukkit.plugin.java.JavaPlugin;

public final class VortexiaCore extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("VortexiaCore has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("VortexiaCore has been disabled!");
    }
}
