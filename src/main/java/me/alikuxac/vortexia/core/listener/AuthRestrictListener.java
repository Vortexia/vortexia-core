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

  private boolean shouldRestrict(Player player) {
    // If player is authenticated by Vortexia PIN system, don't restrict
    if (plugin.getSecurityManager().isAuthenticated(player)) {
        return false;
    }

    // If an external auth plugin (like AuthMe) is still waiting for login, 
    // we DON'T restrict here to avoid blocking AuthMe commands (/login, /register)
    // and to avoid double-blocking movement/chat.
    if (plugin.getAuthHookManager().isWaitingForLogin(player)) {
        return false;
    }

    // Otherwise, restrict (player has passed AuthMe but not yet Vortexia PIN)
    return true;
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onMove(PlayerMoveEvent event) {
    if (shouldRestrict(event.getPlayer())) {
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
        Component.text("Please use /pin verify <pin> to continue", NamedTextColor.GRAY),
        Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofSeconds(1)));
    player.showTitle(title);
  }
}
