// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.command;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import me.alikuxac.vortexia.core.VortexiaCore;
import me.alikuxac.vortexia.core.storage.model.Identity;
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
          player.sendMessage(Component.text("Usage: /pin <setup|verify> <digits>", NamedTextColor.RED));
        })
        .withSubcommand(setupSubcommand())
        .withSubcommand(verifySubcommand())
        .register();
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
                  Component.text("PIN already set! Use /pin verify if you need to log in.", NamedTextColor.RED));
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
              player.sendMessage(Component.text("No PIN found for this account. Use /pin setup.", NamedTextColor.RED));
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
