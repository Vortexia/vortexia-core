// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.command;

import dev.jorel.commandapi.CommandAPICommand;

public interface SubCommand {
  CommandAPICommand getSubcommandBuilder();
}
