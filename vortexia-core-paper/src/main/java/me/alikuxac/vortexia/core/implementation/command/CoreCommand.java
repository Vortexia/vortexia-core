// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.implementation.command;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.LongArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import me.alikuxac.vortexia.core.VortexiaCore;
import me.alikuxac.vortexia.core.core.permission.Permission;
import me.alikuxac.vortexia.core.core.storage.util.InventorySerializer;
import me.alikuxac.vortexia.core.utils.UpdateChecker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CoreCommand implements BaseCommand {

  private final List<SubCommand> subCommands = new ArrayList<>();

  public CoreCommand() {
    this.registerSubCommand(this::getReloadSubcommand);
    this.registerSubCommand(this::getUpdateSubcommand);
    this.registerSubCommand(this::getRestoreSubcommand);
  }

  public void registerSubCommand(SubCommand sub) {
    subCommands.add(sub);
  }

  @Override
  public void register() {
    CommandAPICommand root = new CommandAPICommand("vortexia")
        .withAliases("vx")
        .executes(this::execute);

    for (SubCommand sub : subCommands) {
      root.withSubcommand(sub.getSubcommandBuilder());
    }

    root.register();
  }

  private void execute(CommandSender sender, CommandArguments args) {
    sender.sendMessage("§bVortexiaCore §7version " + VortexiaCore.getInstance().getPluginMeta().getVersion());
    sender.sendMessage("§7Use §f/vortexia help §7to see available commands.");
  }

  private CommandAPICommand getRestoreSubcommand() {
    return new CommandAPICommand("restore")
        .withPermission(Permission.COMMAND_RESTORE.getNode())
        .withArguments(new StringArgument("cccd"))
        .withArguments(new LongArgument("timestamp"))
        .executes((sender, args) -> {
          String cccd = (String) args.get("cccd");
          long timestamp = (long) args.get("timestamp");

          VortexiaCore core = VortexiaCore.getInstance();
          sender.sendMessage("§7[Vortexia] Searching for inventory snapshot for CID: §f" + cccd + " §7at timestamp: §f" + timestamp);

          core.getStorageManager().getStorage().getInventorySnapshotByTimestamp(cccd, timestamp).thenAccept(optSnapshot -> {
            core.getSchedulerService().runGlobal(() -> {
              if (optSnapshot.isEmpty()) {
                sender.sendMessage("§c[Vortexia] No inventory snapshot found for the specified CID and timestamp.");
                return;
              }

              core.getStorageManager().getIdentityByCitizenId(cccd).thenAccept(optIdentity -> {
                core.getSchedulerService().runGlobal(() -> {
                  if (optIdentity.isEmpty()) {
                    sender.sendMessage("§c[Vortexia] No identity account associated with this CID.");
                    return;
                  }

                  Player targetPlayer = Bukkit.getPlayer(optIdentity.get().getUuid());
                  if (targetPlayer == null || !targetPlayer.isOnline()) {
                    if (optIdentity.get().getPremiumUuid() != null) {
                      targetPlayer = Bukkit.getPlayer(optIdentity.get().getPremiumUuid());
                    }
                  }

                  if (targetPlayer == null || !targetPlayer.isOnline()) {
                    sender.sendMessage("§c[Vortexia] The player associated with this CID is not online.");
                    return;
                  }

                  final Player finalPlayer = targetPlayer;
                  try {
                    ItemStack[] inv = InventorySerializer.itemStackArrayFromBase64(optSnapshot.get().getInventoryData());
                    ItemStack[] ec = InventorySerializer.itemStackArrayFromBase64(optSnapshot.get().getEnderChestData());

                    core.getSchedulerService().runEntity(finalPlayer, () -> {
                      finalPlayer.getInventory().setContents(inv);
                      finalPlayer.getEnderChest().setContents(ec);
                      finalPlayer.sendMessage(Component.text("Your inventory has been restored by an Administrator.", NamedTextColor.GREEN));
                      sender.sendMessage("§a[Vortexia] Successfully restored inventory for player §f" + finalPlayer.getName());
                    });
                  } catch (Exception e) {
                    sender.sendMessage("§c[Vortexia] Failed to deserialize inventory data: " + e.getMessage());
                    core.getLoggerService().error("Failed to restore inventory via command: " + e.getMessage());
                  }
                });
              });
            });
          });
        });
  }

  private CommandAPICommand getReloadSubcommand() {
    return new CommandAPICommand("reload")
        .withPermission(Permission.ADMIN.getNode())
        .executes((sender, args) -> {
          VortexiaCore.getInstance().getConfigManager().reload();
          sender.sendMessage("§a[Vortexia] Configuration has been reloaded!");
        });
  }

  private CommandAPICommand getUpdateSubcommand() {
    return new CommandAPICommand("update")
        .withPermission(Permission.ADMIN.getNode())
        .executes((sender, args) -> {
          VortexiaCore core = VortexiaCore.getInstance();
          UpdateChecker checker = core.getUpdateChecker();
          if (checker == null) {
            sender.sendMessage("§c[Vortexia] Update checker is disabled in config.");
            return;
          }
          sender.sendMessage("§7[Vortexia] Checking for updates...");
          checker.checkAsync().thenRun(() -> {
            core.getSchedulerService().runGlobal(() -> {
              if (!checker.hasChecked()) {
                sender.sendMessage("§c[Vortexia] Update check timed out. Try again later.");
                return;
              }
              if (checker.isUpdateAvailable()) {
                sender.sendMessage("§eVortexiaCore update available: §fv" + checker.getLatestVersion() + " §7(current: v" + checker.getCurrentVersion() + ")");
                sender.sendMessage("§7Download: §fhttps://github.com/Vortexia/vortexia-Core/releases/latest");
              } else {
                sender.sendMessage("§a[Vortexia] VortexiaCore is up to date! (v" + checker.getCurrentVersion() + ")");
              }
            });
          });
        });
  }
}
