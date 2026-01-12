// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.event;

import me.alikuxac.vortexia.core.storage.model.Identity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class IdentityLinkEvent extends Event implements Cancellable {

  private static final HandlerList HANDLERS = new HandlerList();
  private boolean cancelled = false;

  private final UUID oldUuid;
  private final UUID newPremiumUuid;
  private final String playerName;
  private final Identity identity;

  public IdentityLinkEvent(UUID oldUuid, UUID newPremiumUuid, String playerName, Identity identity) {
    this.oldUuid = oldUuid;
    this.newPremiumUuid = newPremiumUuid;
    this.playerName = playerName;
    this.identity = identity;
  }

  public UUID getOldUuid() {
    return oldUuid;
  }

  public UUID getNewPremiumUuid() {
    return newPremiumUuid;
  }

  public String getPlayerName() {
    return playerName;
  }

  public Identity getIdentity() {
    return identity;
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public void setCancelled(boolean cancel) {
    this.cancelled = cancel;
  }

  @NotNull
  @Override
  public HandlerList getHandlers() {
    return HANDLERS;
  }

  @NotNull
  public static HandlerList getHandlerList() {
    return HANDLERS;
  }
}
