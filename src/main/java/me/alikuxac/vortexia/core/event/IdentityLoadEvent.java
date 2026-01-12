// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.event;

import me.alikuxac.vortexia.core.storage.model.Identity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class IdentityLoadEvent extends Event {

  private static final HandlerList HANDLERS = new HandlerList();

  private final Identity identity;
  private final LoadSource source;

  public enum LoadSource {
    DATABASE,
    CACHE,
    MIGRATION
  }

  public IdentityLoadEvent(Identity identity, LoadSource source) {
    super(true);
    this.identity = identity;
    this.source = source;
  }

  public Identity getIdentity() {
    return identity;
  }

  public LoadSource getSource() {
    return source;
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
