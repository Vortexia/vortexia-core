// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.implementation.command;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import me.alikuxac.vortexia.core.VortexiaCore;
import me.alikuxac.vortexia.core.core.permission.Permission;
import me.alikuxac.vortexia.api.model.Identity;
import me.alikuxac.vortexia.core.implementation.gui.ProfileGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ProfileCommand implements BaseCommand {

  private final VortexiaCore plugin;

  public ProfileCommand(VortexiaCore plugin) {
    this.plugin = plugin;
  }

  @Override
  public void register() {
    new CommandAPICommand("cccd")
        .withAliases("profile", "id")
        .withPermission(Permission.COMMAND_CCCD.getNode())
        .executesPlayer((player, args) -> {
          handleView(player, null);
        })
        .withSubcommand(new CommandAPICommand("view")
            .withPermission(Permission.COMMAND_CCCD_VIEW.getNode())
            .withOptionalArguments(new EntitySelectorArgument.OnePlayer("target"))
            .executesPlayer((player, args) -> {
              Player target = (Player) args.get("target");
              handleView(player, target);
            }))
        .withSubcommand(new CommandAPICommand("get")
            .withAliases("withdraw")
            .withPermission(Permission.COMMAND_CCCD_GET.getNode())
            .executesPlayer((player, args) -> {
              giveCard(player);
            }))
        .withSubcommand(new CommandAPICommand("give")
            .withPermission(Permission.COMMAND_CCCD_GIVE.getNode())
            .withArguments(new EntitySelectorArgument.OnePlayer("target"))
            .executesPlayer((player, args) -> {
              Player target = (Player) args.get("target");
              giveCardTo(player, target);
            }))
        .register();
  }

  private void handleView(Player viewer, Player target) {
    if (target != null) {
      showProfile(viewer, target);
      return;
    }

    ItemStack item = viewer.getInventory().getItemInMainHand();
    if (plugin.getCitizenCardManager().isCitizenCard(item)) {
      String cid = plugin.getCitizenCardManager().getCitizenId(item);
      if (cid != null) {
        plugin.getStorageManager().getIdentityByCitizenId(cid).thenAccept(optIdentity -> {
          if (optIdentity.isPresent()) {
            plugin.getSchedulerService().runEntity(viewer, () -> {
              new ProfileGUI(plugin).open(viewer, optIdentity.get());
            });
          } else {
            viewer.sendMessage(Component.text("Identity on card not found in database.", NamedTextColor.RED));
          }
        });
        return;
      }
    }

    showProfile(viewer, viewer);
  }

  private void showProfile(Player viewer, Player target) {
    plugin.getStorageManager().getIdentity(target.getUniqueId()).thenAccept(optIdentity -> {
      if (optIdentity.isEmpty()) {
        viewer.sendMessage(Component.text("Identity not found for this player.", NamedTextColor.RED));
        return;
      }

      plugin.getSchedulerService().runEntity(viewer, () -> {
        new ProfileGUI(plugin).open(viewer, optIdentity.get());
      });
    });
  }

  private void giveCard(Player player) {
    giveCardTo(player, player);
  }

  private void giveCardTo(Player issuer, Player target) {
    if (target == null) {
      issuer.sendMessage(Component.text("Target player not found or offline.", NamedTextColor.RED));
      return;
    }

    plugin.getStorageManager().getIdentity(target.getUniqueId()).thenAccept(optIdentity -> {
      if (optIdentity.isEmpty()) {
        issuer.sendMessage(Component.text("Identity not found for " + target.getName() + "!", NamedTextColor.RED));
        return;
      }

      Identity identity = optIdentity.get();
      if (identity.getCitizenId() == null || identity.getCitizenId().isEmpty()) {
        issuer.sendMessage(Component.text("Target player does not have a Citizen ID yet.", NamedTextColor.RED));
        return;
      }

      if (target.getInventory().firstEmpty() == -1) {
        issuer.sendMessage(Component.text("Cannot issue card: " + target.getName() + "'s inventory is full!", NamedTextColor.RED));
        target.sendMessage(Component.text("An administrator tried to issue your Citizen ID Card, but your inventory is full!", NamedTextColor.RED));
        return;
      }

      ItemStack card = plugin.getCitizenCardManager().createCard(identity, target);
      target.getInventory().addItem(card);
      target.sendMessage(Component.text("You have received your Citizen ID Card.", NamedTextColor.GREEN));
      if (!issuer.getUniqueId().equals(target.getUniqueId())) {
        issuer.sendMessage(Component.text("Successfully issued Citizen ID Card to " + target.getName() + ".", NamedTextColor.GREEN));
      }
    });
  }
}
