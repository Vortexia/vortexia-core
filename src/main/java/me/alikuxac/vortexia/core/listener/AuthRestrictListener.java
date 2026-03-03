// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.listener;

import me.alikuxac.vortexia.core.VortexiaCore;
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

  @EventHandler(priority = EventPriority.LOWEST)
  public void onMove(PlayerMoveEvent event) {
    if (!plugin.getSecurityManager().isAuthenticated(event.getPlayer())) {
      // Allow looking around but not moving
      if (event.getFrom().getX() != event.getTo().getX() ||
          event.getFrom().getZ() != event.getTo().getZ() ||
          event.getFrom().getY() != event.getTo().getY()) {
        event.setTo(event.getFrom());
        sendAuthReminder(event.getPlayer());
      }
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onChat(io.papermc.paper.event.player.AsyncChatEvent event) {
    if (!plugin.getSecurityManager().isAuthenticated(event.getPlayer())) {
      event.setCancelled(true);
      event.getPlayer().sendMessage(Component.text("You must authenticate before chatting!", NamedTextColor.RED));
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onInteract(PlayerInteractEvent event) {
    if (!plugin.getSecurityManager().isAuthenticated(event.getPlayer())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onBreak(BlockBreakEvent event) {
    if (!plugin.getSecurityManager().isAuthenticated(event.getPlayer())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPlace(BlockPlaceEvent event) {
    if (!plugin.getSecurityManager().isAuthenticated(event.getPlayer())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onCommand(PlayerCommandPreprocessEvent event) {
    String cmd = event.getMessage().toLowerCase();
    if (!plugin.getSecurityManager().isAuthenticated(event.getPlayer())) {
      if (!cmd.startsWith("/pin") && !cmd.startsWith("/vortexia:pin")) {
        event.setCancelled(true);
        event.getPlayer()
            .sendMessage(Component.text("You must authenticate before using this command!", NamedTextColor.RED));
      }
    }
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    plugin.getSecurityManager().clear(event.getPlayer());
  }

  private void sendAuthReminder(Player player) {
    // We use a cooldown logic or just send it sparingly
    // For simplicity, we send a title every few moves
    Title title = Title.title(
        Component.text("AUTHENTICATION REQUIRED", NamedTextColor.RED),
        Component.text("Please use /pin to secure your account", NamedTextColor.GRAY),
        Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofSeconds(1)));
    player.showTitle(title);
  }
}
