// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.command;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import me.alikuxac.vortexia.core.VortexiaCore;
import me.alikuxac.vortexia.core.storage.model.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ProfileCommand implements BaseCommand {

  private final VortexiaCore plugin;
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy")
      .withZone(ZoneId.systemDefault());

  public ProfileCommand(VortexiaCore plugin) {
    this.plugin = plugin;
  }

  @Override
  public void register() {
    new CommandAPICommand("cccd")
        .withAliases("profile", "id")
        .withPermission("vortexia.command.cccd")
        .executesPlayer((player, args) -> {
          showProfile(player, player);
        })
        .withSubcommand(new CommandAPICommand("view")
            .withPermission("vortexia.command.cccd.view")
            .withArguments(new EntitySelectorArgument.OnePlayer("target"))
            .executesPlayer((player, args) -> {
              Player target = (Player) args.get("target");
              showProfile(player, target);
            }))
        .withSubcommand(new CommandAPICommand("get")
            .withAliases("withdraw")
            .withPermission("vortexia.command.cccd.get")
            .executesPlayer((player, args) -> {
              giveCard(player);
            }))
        .register();
  }

  private void showProfile(Player viewer, Player target) {
    plugin.getStorageManager().getIdentity(target.getUniqueId()).thenAccept(optIdentity -> {
      if (optIdentity.isEmpty()) {
        viewer.sendMessage(Component.text("Identity not found for this player.", NamedTextColor.RED));
        return;
      }

      Identity identity = optIdentity.get();
      String citizenId = identity.getCitizenId() != null ? identity.getCitizenId() : "Pending...";
      String name = identity.getName();
      String date = DATE_FORMATTER.format(Instant.ofEpochMilli(identity.getCreatedAt()));

      viewer.sendMessage(Component.text("⎯⎯⎯⎯⎯⎯ [ Citizen Profile ] ⎯⎯⎯⎯⎯⎯", NamedTextColor.GOLD));
      viewer.sendMessage(Component.text("Name: ", NamedTextColor.GRAY)
          .append(Component.text(name, NamedTextColor.WHITE)));
      viewer.sendMessage(Component.text("Citizen ID: ", NamedTextColor.GRAY)
          .append(Component.text(citizenId, NamedTextColor.YELLOW)));
      viewer.sendMessage(Component.text("Issued Date: ", NamedTextColor.GRAY)
          .append(Component.text(date, NamedTextColor.WHITE)));
      viewer.sendMessage(Component.text("⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯", NamedTextColor.GOLD));
    });
  }

  private void giveCard(Player player) {
    plugin.getStorageManager().getIdentity(player.getUniqueId()).thenAccept(optIdentity -> {
      if (optIdentity.isEmpty()) {
        player.sendMessage(Component.text("Identity not found! Please rejoin.", NamedTextColor.RED));
        return;
      }

      Identity identity = optIdentity.get();
      if (identity.getCitizenId() == null) {
        player.sendMessage(Component.text("You do not have a Citizen ID yet.", NamedTextColor.RED));
        return;
      }

      // Check inventory space
      if (player.getInventory().firstEmpty() == -1) {
        player.sendMessage(Component.text("Your inventory is full!", NamedTextColor.RED));
        return;
      }

      org.bukkit.inventory.ItemStack card = plugin.getCitizenCardManager().createCard(identity);
      player.getInventory().addItem(card);
      player.sendMessage(Component.text("You have received your Citizen ID Card.", NamedTextColor.GREEN));
    });
  }
}
