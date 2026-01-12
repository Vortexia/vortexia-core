// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.command;

import dev.jorel.commandapi.CommandAPICommand;

public class GeneralModuleCommand implements SubCommand {

  @Override
  public CommandAPICommand getSubcommandBuilder() {
    return new CommandAPICommand("info")
        .withSubcommand(new CommandAPICommand("link")
            .executes((sender, args) -> {
              sender.sendMessage("§b--- Vortexia Links ---");
              sender.sendMessage("§fDiscord: §7Not found");
              sender.sendMessage("§fWiki: §7Not found");
              sender.sendMessage("§fStore: §7Not found");
            }))
        .withSubcommand(new CommandAPICommand("guide")
            .executes((sender, args) -> {
              sender.sendMessage("§6--- Vortexia Guide ---");
              sender.sendMessage("§fWelcome to Vortexia! Use §e/vx info link §ffor more info.");
              sender.sendMessage("§fCheck out our tutorial at the spawn area.");
            }));
  }
}
