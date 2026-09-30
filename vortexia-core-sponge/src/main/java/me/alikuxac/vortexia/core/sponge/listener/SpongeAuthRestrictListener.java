// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.sponge.listener;

import me.alikuxac.vortexia.core.sponge.VortexiaSponge;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.command.ExecuteCommandEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.message.PlayerChatEvent;

public class SpongeAuthRestrictListener {

    private final VortexiaSponge plugin;

    public SpongeAuthRestrictListener(VortexiaSponge plugin) {
        this.plugin = plugin;
    }

    private boolean shouldRestrict(ServerPlayer player) {
        return !plugin.getSecurityManager().isAuthenticated(player.uniqueId());
    }

    @Listener(order = Order.FIRST)
    public void onMove(MoveEntityEvent event, @First ServerPlayer player) {
        if (shouldRestrict(player)) {
            event.setCancelled(true);
        }
    }

    @Listener(order = Order.FIRST)
    public void onChat(PlayerChatEvent event, @First ServerPlayer player) {
        if (shouldRestrict(player)) {
            if (event instanceof org.spongepowered.api.event.Cancellable cancellable) {
                cancellable.setCancelled(true);
            }
            player.sendMessage(Component.text("You must authenticate your PIN before chatting!", NamedTextColor.RED));
        }
    }

    @Listener(order = Order.FIRST)
    public void onBlockBreak(ChangeBlockEvent.All event, @First ServerPlayer player) {
        if (shouldRestrict(player)) {
            event.setCancelled(true);
        }
    }

    @Listener(order = Order.FIRST)
    public void onCommand(ExecuteCommandEvent.Pre event, @First ServerPlayer player) {
        String command = event.command().toLowerCase();
        if (shouldRestrict(player)) {
            if (!command.startsWith("pin") && !command.startsWith("vortexia:pin")) {
                event.setCancelled(true);
                player.sendMessage(Component.text("You must verify your PIN before using commands!", NamedTextColor.RED));
            }
        }
    }
}
