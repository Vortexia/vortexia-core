// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.service;

import me.alikuxac.vortexia.core.VortexiaCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import java.util.logging.Level;

public class LoggerService {

  private final VortexiaCore plugin;
  private final String prefix;
  private boolean debug;

  public LoggerService(VortexiaCore plugin) {
    this.plugin = plugin;
    this.prefix = plugin.getConfig().getString("prefix", "&8[&bVortexia&8] &r");
    this.debug = plugin.getConfig().getBoolean("debug", false);
  }

  public void info(String message) {
    log(Level.INFO, message);
  }

  public void warn(String message) {
    log(Level.WARNING, message);
  }

  public void error(String message) {
    log(Level.SEVERE, message);
  }

  public void debug(String message) {
    if (debug) {
      log(Level.INFO, "&7[DEBUG] " + message);
    }
  }

  public void setDebug(boolean debug) {
    this.debug = debug;
  }

  private void log(Level level, String message) {
    Component coloredMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + message);
    plugin.getServer().getConsoleSender().sendMessage(coloredMessage);
  }
}
