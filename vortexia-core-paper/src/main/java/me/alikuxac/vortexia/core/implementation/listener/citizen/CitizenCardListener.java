// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.implementation.listener.citizen;

import me.alikuxac.vortexia.core.VortexiaCore;
import me.alikuxac.vortexia.core.implementation.gui.ProfileGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class CitizenCardListener implements Listener {

  private final VortexiaCore plugin;

  public CitizenCardListener(VortexiaCore plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onAnvilPrepare(PrepareAnvilEvent event) {
    ItemStack first = event.getInventory().getFirstItem();
    ItemStack second = event.getInventory().getSecondItem();

    if (plugin.getCitizenCardManager().isCitizenCard(first) || plugin.getCitizenCardManager().isCitizenCard(second)) {
      event.setResult(null);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onCraftPrepare(PrepareItemCraftEvent event) {
    for (ItemStack matrixItem : event.getInventory().getMatrix()) {
      if (matrixItem != null && plugin.getCitizenCardManager().isCitizenCard(matrixItem)) {
        event.getInventory().setResult(null);
        return;
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onGrindstonePrepare(PrepareGrindstoneEvent event) {
    ItemStack upper = event.getInventory().getItem(0);
    ItemStack lower = event.getInventory().getItem(1);

    if (plugin.getCitizenCardManager().isCitizenCard(upper) || plugin.getCitizenCardManager().isCitizenCard(lower)) {
      event.setResult(null);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onSmithingPrepare(PrepareSmithingEvent event) {
    for (int i = 0; i < 3; i++) {
      ItemStack item = event.getInventory().getItem(i);
      if (item != null && plugin.getCitizenCardManager().isCitizenCard(item)) {
        event.setResult(null);
        return;
      }
    }
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
      return;
    }

    ItemStack item = event.getItem();
    if (plugin.getCitizenCardManager().isCitizenCard(item)) {
      String cid = plugin.getCitizenCardManager().getCitizenId(item);
      if (cid == null) return;

      event.setCancelled(true);
      
      plugin.getStorageManager().getIdentityByCitizenId(cid).thenAccept(optIdentity -> {
        if (optIdentity.isPresent()) {
          plugin.getSchedulerService().runEntity(event.getPlayer(), () -> {
            new ProfileGUI(plugin).open(event.getPlayer(), optIdentity.get());
          });
        } else {
          event.getPlayer().sendMessage(Component.text("Identity on card not found.", NamedTextColor.RED));
        }
      });
    }
  }
}
