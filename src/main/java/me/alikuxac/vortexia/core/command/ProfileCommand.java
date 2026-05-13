// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.command;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import me.alikuxac.vortexia.core.VortexiaCore;
import me.alikuxac.vortexia.api.model.Identity;
import me.alikuxac.vortexia.core.gui.ProfileGUI;
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
        .withPermission("vortexia.command.cccd")
        .executesPlayer((player, args) -> {
          handleView(player, null);
        })
        .withSubcommand(new CommandAPICommand("view")
            .withPermission("vortexia.command.cccd.view")
            .withOptionalArguments(new EntitySelectorArgument.OnePlayer("target"))
            .executesPlayer((player, args) -> {
              Player target = (Player) args.get("target");
              handleView(player, target);
            }))
        .withSubcommand(new CommandAPICommand("get")
            .withAliases("withdraw")
            .withPermission("vortexia.command.cccd.get")
            .executesPlayer((player, args) -> {
              giveCard(player);
            }))
        .register();
  }

  private void handleView(Player viewer, Player target) {
    if (target != null) {
      showProfile(viewer, target);
      return;
    }

    // Check if holding a card
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

    // Default to self
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
