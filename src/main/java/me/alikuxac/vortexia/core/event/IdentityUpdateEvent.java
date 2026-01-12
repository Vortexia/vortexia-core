// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.event;

import me.alikuxac.vortexia.core.storage.model.Identity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IdentityUpdateEvent extends Event implements Cancellable {

  private static final HandlerList HANDLERS = new HandlerList();
  private boolean cancelled = false;

  private final Identity oldIdentity;
  private final Identity newIdentity;
  private final UpdateType updateType;

  public enum UpdateType {
    PIN_CHANGE,
    PREMIUM_UUID_LINK,
    PREMIUM_UUID_UNLINK,
    METADATA_CHANGE,
    FULL_UPDATE
  }

  public IdentityUpdateEvent(@Nullable Identity oldIdentity, Identity newIdentity, UpdateType updateType) {
    this.oldIdentity = oldIdentity;
    this.newIdentity = newIdentity;
    this.updateType = updateType;
  }

  @Nullable
  public Identity getOldIdentity() {
    return oldIdentity;
  }

  public Identity getNewIdentity() {
    return newIdentity;
  }

  public UpdateType getUpdateType() {
    return updateType;
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
