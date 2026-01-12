package com.vortexia.core.config;

import com.vortexia.core.VortexiaCore;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

  private final VortexiaCore plugin;

  public ConfigManager(VortexiaCore plugin) {
    this.plugin = plugin;
  }

  public void loadConfig() {
    plugin.saveDefaultConfig();
    plugin.reloadConfig();
  }

  public void reload() {
    plugin.reloadConfig();
    plugin.getLoggerService().setDebug(plugin.getConfig().getBoolean("debug", false));
  }

  public FileConfiguration getConfig() {
    return plugin.getConfig();
  }
}
