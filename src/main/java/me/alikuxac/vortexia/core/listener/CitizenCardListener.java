// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.listener;

import me.alikuxac.vortexia.core.VortexiaCore;
import me.alikuxac.vortexia.core.gui.ProfileGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
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
          org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            new ProfileGUI(plugin).open(event.getPlayer(), optIdentity.get());
          });
        } else {
          event.getPlayer().sendMessage(Component.text("Identity on card not found.", NamedTextColor.RED));
        }
      });
    }
  }
}
