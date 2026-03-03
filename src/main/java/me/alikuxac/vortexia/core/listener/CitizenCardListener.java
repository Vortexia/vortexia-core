// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.listener;

import me.alikuxac.vortexia.core.VortexiaCore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;

public class CitizenCardListener implements Listener {

  private final VortexiaCore plugin;

  public CitizenCardListener(VortexiaCore plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onAnvilPrepare(PrepareAnvilEvent event) {
    ItemStack first = event.getInventory().getFirstItem();

    if (plugin.getCitizenCardManager().isCitizenCard(first)) {
      // Prevent using Citizen Card in Anvil (Renaming or Enchanting)
      event.setResult(null);
    }
  }
}
