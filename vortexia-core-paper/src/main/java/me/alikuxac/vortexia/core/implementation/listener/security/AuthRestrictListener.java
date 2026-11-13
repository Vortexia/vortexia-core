// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.implementation.listener.security;

import me.alikuxac.vortexia.core.VortexiaCore;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.*;

import java.time.Duration;

public class AuthRestrictListener implements Listener {

  private final VortexiaCore plugin;

  public AuthRestrictListener(VortexiaCore plugin) {
    this.plugin = plugin;
  }

  private boolean shouldRestrict(Player player) {
    if (plugin.getSecurityManager().isAuthenticated(player)) {
        return false;
    }

    if (plugin.getAuthHookManager() != null && plugin.getAuthHookManager().isWaitingForLogin(player)) {
        return false;
    }

    return true;
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onMove(PlayerMoveEvent event) {
    if (shouldRestrict(event.getPlayer())) {
      if (event.getFrom().getX() != event.getTo().getX() ||
          event.getFrom().getZ() != event.getTo().getZ() ||
          event.getFrom().getY() != event.getTo().getY()) {
        event.setTo(event.getFrom());
        sendAuthReminder(event.getPlayer());
      }
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onChat(AsyncChatEvent event) {
    if (shouldRestrict(event.getPlayer())) {
      event.setCancelled(true);
      event.getPlayer().sendMessage(Component.text("You must authenticate your PIN before chatting!", NamedTextColor.RED));
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onInteract(PlayerInteractEvent event) {
    if (shouldRestrict(event.getPlayer())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onBreak(BlockBreakEvent event) {
    if (shouldRestrict(event.getPlayer())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPlace(BlockPlaceEvent event) {
    if (shouldRestrict(event.getPlayer())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPickup(org.bukkit.event.entity.EntityPickupItemEvent event) {
    if (event.getEntity() instanceof Player player && shouldRestrict(player)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onDrop(PlayerDropItemEvent event) {
    if (shouldRestrict(event.getPlayer())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
    if (event.getWhoClicked() instanceof Player player && shouldRestrict(player)) {
      org.bukkit.inventory.Inventory top = event.getView().getTopInventory();
      if (top.getHolder() != null && top.getHolder().getClass().getName().contains("SecurityGUIHolder")) {
        return;
      }
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onInventoryDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
    if (event.getWhoClicked() instanceof Player player && shouldRestrict(player)) {
      org.bukkit.inventory.Inventory top = event.getView().getTopInventory();
      if (top.getHolder() != null && top.getHolder().getClass().getName().contains("SecurityGUIHolder")) {
        return;
      }
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
    if (shouldRestrict(event.getPlayer())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onEntityDamage(org.bukkit.event.entity.EntityDamageEvent event) {
    if (event.getEntity() instanceof Player player && shouldRestrict(player)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onEntityDamageByEntity(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
    if (event.getDamager() instanceof Player player && shouldRestrict(player)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onCommand(PlayerCommandPreprocessEvent event) {
    String cmd = event.getMessage().toLowerCase();
    if (shouldRestrict(event.getPlayer())) {
      if (!cmd.startsWith("/pin") && !cmd.startsWith("/vortexia:pin")) {
        event.setCancelled(true);
        event.getPlayer()
            .sendMessage(Component.text("You must verify your PIN before using this command!", NamedTextColor.RED));
      }
    }
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    plugin.getSecurityManager().clear(event.getPlayer());
  }

  private void sendAuthReminder(Player player) {
    Title title = Title.title(
        Component.text("PIN VERIFICATION REQUIRED", NamedTextColor.RED),
        Component.text("Please enter your PIN", NamedTextColor.GRAY),
        Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofSeconds(1)));
    player.showTitle(title);

    if (plugin.getSecurityManager().pinLockoutSecondsLeft(player) <= 0) {
      org.bukkit.inventory.Inventory top = player.getOpenInventory().getTopInventory();
      if (top.getHolder() == null || !top.getHolder().getClass().getName().contains("SecurityGUIHolder")) {
        plugin.getStorageManager().getIdentity(player.getUniqueId()).thenAccept(opt -> {
          if (opt.isPresent() && opt.get().getPin() != null && !opt.get().getPin().isEmpty()) {
            plugin.getSecurityGUI().openVerify(player);
          } else {
            plugin.getSecurityGUI().openSetup(player);
          }
        });
      }
    }
  }
}
