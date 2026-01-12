package com.vortexia.core.command;

import com.vortexia.core.VortexiaCore;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.command.CommandSender;
import java.util.ArrayList;
import java.util.List;

public class CoreCommand implements BaseCommand {

  private final List<SubCommand> subCommands = new ArrayList<>();

  public CoreCommand() {
    // Đăng ký các subcommand cục bộ tại đây
    this.registerSubCommand(this::getReloadSubcommand);
  }

  /**
   * Cho phép các module khác đăng ký thêm subcommand vào lệnh /vx
   */
  public void registerSubCommand(SubCommand sub) {
    subCommands.add(sub);
  }

  @Override
  public void register() {
    CommandAPICommand root = new CommandAPICommand("vortexia")
        .withAliases("vx")
        .executes(this::execute);

    // Tự động gắn tất cả subcommand đã đăng ký
    for (SubCommand sub : subCommands) {
      root.withSubcommand(sub.getSubcommandBuilder());
    }

    root.register();
  }

  private void execute(CommandSender sender, CommandArguments args) {
    sender.sendMessage("§bVortexiaCore §7version " + VortexiaCore.getInstance().getPluginMeta().getVersion());
    sender.sendMessage("§7Use §f/vortexia help §7to see available commands.");
  }

  // Một ví dụ về việc tạo builder cho subcommand
  private CommandAPICommand getReloadSubcommand() {
    return new CommandAPICommand("reload")
        .withPermission("vortexia.admin")
        .executes((sender, args) -> {
          VortexiaCore.getInstance().getConfigManager().reload();
          sender.sendMessage("§a[Vortexia] Configuration has been reloaded!");
        });
  }
}
