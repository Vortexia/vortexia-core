// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.command;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import me.alikuxac.vortexia.core.VortexiaCore;
import me.alikuxac.vortexia.api.model.Identity;
import me.alikuxac.vortexia.core.util.PINUtil;
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
        .withPermission("vortexia.command.pin")
        .executesPlayer((player, args) -> {
          plugin.getStorageManager().getIdentity(player.getUniqueId()).thenAccept(optIdentity -> {
            if (optIdentity.isPresent() && optIdentity.get().getPin() != null && !optIdentity.get().getPin().isEmpty()) {
              if (plugin.getSecurityManager().isAuthenticated(player)) {
                player.sendMessage(Component.text("You are already authenticated. Use /pin change to update your PIN.", NamedTextColor.GREEN));
              } else {
                player.sendMessage(Component.text("Please authenticate using /pin verify <your_pin>", NamedTextColor.YELLOW));
              }
            } else {
              player.sendMessage(Component.text("Please set up your security PIN using /pin setup <new_pin>", NamedTextColor.YELLOW));
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
        .withArguments(new StringArgument("old_pin"))
        .withArguments(new StringArgument("new_pin"))
        .executesPlayer((player, args) -> {
          String oldPin = (String) args.get("old_pin");
          String newPin = (String) args.get("new_pin");

          if (newPin == null || newPin.length() < 4) {
            player.sendMessage(Component.text("New PIN must be at least 4 digits!", NamedTextColor.RED));
            return;
          }

          plugin.getStorageManager().getIdentity(player.getUniqueId()).thenAccept(optIdentity -> {
            if (optIdentity.isEmpty() || optIdentity.get().getPin() == null) {
              player.sendMessage(Component.text("No PIN found. Use /pin setup <pin> first.", NamedTextColor.RED));
              return;
            }

            String storedHash = optIdentity.get().getPin();
            if (PINUtil.verify(oldPin, storedHash)) {
              String newHashed = PINUtil.hash(newPin);
              plugin.getIdentityMigrationHelper().createOrUpdateIdentity(player, newHashed).thenRun(() -> {
                player.sendMessage(Component.text("PIN changed successfully!", NamedTextColor.GREEN));
              });
            } else {
              player.sendMessage(Component.text("Incorrect current PIN!", NamedTextColor.RED));
            }
          });
        });
  }

  private CommandAPICommand setupSubcommand() {
    return new CommandAPICommand("setup")
        .withArguments(new StringArgument("new_pin"))
        .executesPlayer((player, args) -> {
          String pin = (String) args.get("new_pin");

          if (pin == null || pin.length() < 4) {
            player.sendMessage(Component.text("PIN must be at least 4 digits!", NamedTextColor.RED));
            return;
          }

          plugin.getStorageManager().getIdentity(player.getUniqueId()).thenAccept(optIdentity -> {
            if (optIdentity.isPresent() && optIdentity.get().getPin() != null
                && !optIdentity.get().getPin().isEmpty()) {
              player.sendMessage(
                  Component.text("PIN already set! Use /pin verify <pin> if you need to log in.", NamedTextColor.RED));
              return;
            }

            // Create/Update identity with hashed PIN
            String hashed = PINUtil.hash(pin);
            plugin.getIdentityMigrationHelper().createOrUpdateIdentity(player, hashed).thenRun(() -> {
              plugin.getSecurityManager().authenticate(player);
              player.sendMessage(
                  Component.text("PIN set successfully! You are now authenticated.", NamedTextColor.GREEN));
            });
          });
        });
  }

  private CommandAPICommand verifySubcommand() {
    return new CommandAPICommand("verify")
        .withArguments(new StringArgument("pin"))
        .executesPlayer((player, args) -> {
          String inputPin = (String) args.get("pin");

          plugin.getStorageManager().getIdentity(player.getUniqueId()).thenAccept(optIdentity -> {
            if (optIdentity.isEmpty() || optIdentity.get().getPin() == null) {
              player.sendMessage(Component.text("No PIN found for this account. Use /pin setup <pin>.", NamedTextColor.RED));
              return;
            }

            String storedHash = optIdentity.get().getPin();
            if (PINUtil.verify(inputPin, storedHash)) {
              Identity identity = optIdentity.get();

              // If UUID mismatch, this is a migration case
              if (!identity.getUuid().equals(player.getUniqueId())) {
                plugin.getIdentityMigrationHelper().completeMigration(player, identity).thenAccept(success -> {
                  if (success) {
                    plugin.getSecurityManager().authenticate(player);
                    player.sendMessage(
                        Component.text("Identity migrated and authenticated successfully!", NamedTextColor.GREEN));
                  } else {
                    player.sendMessage(
                        Component.text("Failed to complete migration. Please contact admin.", NamedTextColor.RED));
                  }
                });
              } else {
                plugin.getSecurityManager().authenticate(player);
                player.sendMessage(Component.text("Authentication successful!", NamedTextColor.GREEN));
              }
            } else {
              player.sendMessage(Component.text("Incorrect PIN! Please try again.", NamedTextColor.RED));
            }
          });
        });
  }
}
