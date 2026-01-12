package com.vortexia.core.command;

import dev.jorel.commandapi.CommandAPICommand;

/**
 * Interface dành cho các subcommand của lệnh chính /vortexia
 */
public interface SubCommand {
  /**
   * Trả về builder của subcommand để gắn vào lệnh chính.
   */
  CommandAPICommand getSubcommandBuilder();
}
