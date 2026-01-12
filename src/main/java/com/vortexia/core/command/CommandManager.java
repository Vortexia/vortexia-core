package com.vortexia.core.command;

import com.vortexia.core.VortexiaCore;
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
    // Tạo Root Command chính /vx
    CoreCommand root = new CoreCommand();

    // Gắn các module subcommand vào Root
    root.registerSubCommand(new GeneralModuleCommand());

    // Thêm các lệnh độc lập khác nếu có
    commands.add(root);
  }

  public void registerCommands() {
    for (BaseCommand command : commands) {
      command.register();
    }

    plugin.getLoggerService().info("Registered " + commands.size() + " command groups successfully!");
  }
}
