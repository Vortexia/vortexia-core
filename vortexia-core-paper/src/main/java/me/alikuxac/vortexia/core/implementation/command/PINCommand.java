// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.implementation.command;

import dev.jorel.commandapi.CommandAPICommand;
import me.alikuxac.vortexia.core.VortexiaCore;
import me.alikuxac.vortexia.core.core.permission.Permission;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class PINCommand implements BaseCommand {

  private final VortexiaCore plugin;

  public PINCommand(VortexiaCore plugin) {
    this.plugin = plugin;
  }

  @Override
  public void register() {
    new CommandAPICommand("pin")
        .withAliases("auth", "password")
        .withPermission(Permission.COMMAND_PIN.getNode())
        .executesPlayer((player, args) -> {
          plugin.getStorageManager().getIdentity(player.getUniqueId()).thenAccept(optIdentity -> {
            if (optIdentity.isPresent() && optIdentity.get().getPin() != null && !optIdentity.get().getPin().isEmpty()) {
              if (plugin.getSecurityManager().isAuthenticated(player)) {
                player.sendMessage(Component.text("You are already authenticated. Use /pin change to update your PIN.", NamedTextColor.GREEN));
              } else {
                player.sendMessage(Component.text("Please authenticate using /pin verify", NamedTextColor.YELLOW));
              }
            } else {
              player.sendMessage(Component.text("Please set up your security PIN using /pin setup", NamedTextColor.YELLOW));
            }
          });
        })
        .withSubcommand(setupSubcommand())
        .withSubcommand(verifySubcommand())
        .withSubcommand(changeSubcommand())
        .register();
  }

  private CommandAPICommand changeSubcommand() {
    return new CommandAPICommand("change")
        .executesPlayer((player, args) -> {
          plugin.getStorageManager().getIdentity(player.getUniqueId()).thenAccept(optIdentity -> {
            if (optIdentity.isEmpty() || optIdentity.get().getPin() == null) {
              player.sendMessage(Component.text("No PIN found. Use /pin setup first.", NamedTextColor.RED));
              return;
            }
            plugin.getSchedulerService().runEntity(player, () -> plugin.getSecurityGUI().openChangeVerify(player));
          });
        });
  }

  private CommandAPICommand setupSubcommand() {
    return new CommandAPICommand("setup")
        .executesPlayer((player, args) -> {
          plugin.getStorageManager().getIdentity(player.getUniqueId()).thenAccept(optIdentity -> {
            if (optIdentity.isPresent() && optIdentity.get().getPin() != null
                && !optIdentity.get().getPin().isEmpty()) {
              player.sendMessage(
                  Component.text("PIN already set! Use /pin verify if you need to log in.", NamedTextColor.RED));
              return;
            }
            plugin.getSchedulerService().runEntity(player, () -> plugin.getSecurityGUI().openSetup(player));
          });
        });
  }

  private CommandAPICommand verifySubcommand() {
    return new CommandAPICommand("verify")
        .executesPlayer((player, args) -> {
          if (plugin.getSecurityManager().pinLockoutSecondsLeft(player) > 0) {
            long left = plugin.getSecurityManager().pinLockoutSecondsLeft(player);
            player.sendMessage(Component.text("Too many failed attempts. Try again in " + left + "s.", NamedTextColor.RED));
            return;
          }

          plugin.getStorageManager().getIdentity(player.getUniqueId()).thenAccept(optIdentity -> {
            if (optIdentity.isEmpty() || optIdentity.get().getPin() == null) {
              player.sendMessage(Component.text("No PIN found for this account. Use /pin setup.", NamedTextColor.RED));
              return;
            }
            plugin.getSchedulerService().runEntity(player, () -> plugin.getSecurityGUI().openVerify(player));
          });
        });
  }
}
