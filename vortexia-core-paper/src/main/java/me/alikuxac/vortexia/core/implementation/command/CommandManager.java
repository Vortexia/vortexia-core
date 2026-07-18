// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.implementation.command;

import me.alikuxac.vortexia.core.VortexiaCore;
import java.util.ArrayList;
import java.util.List;

public class CommandManager {

  private final VortexiaCore plugin;
  private final List<BaseCommand> commands = new ArrayList<>();

  public CommandManager(VortexiaCore plugin) {
    this.plugin = plugin;
    this.setup();
  }

  private void setup() {
    CoreCommand root = new CoreCommand();

    root.registerSubCommand(new GeneralModuleCommand());
    root.registerSubCommand(new GuideCommand(plugin));

    // Independent commands
    new ProfileCommand(plugin).register();
    new PINCommand(plugin).register();

    commands.add(root);
  }

  public void registerCommands() {
    for (BaseCommand command : commands) {
      command.register();
    }

    plugin.getLoggerService().info("Registered " + commands.size() + " command groups successfully!");
  }
}
