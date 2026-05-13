// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import me.alikuxac.vortexia.core.VortexiaCore;
import org.bukkit.entity.Player;

public class SecurityPacketListener extends PacketListenerAbstract {

    private final VortexiaCore plugin;

    public SecurityPacketListener(VortexiaCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPlayer() == null) return;
        Player player = (Player) event.getPlayer();

        // If player is not authenticated in Vortexia SecurityManager, cancel interaction packets
        if (!plugin.getSecurityManager().isAuthenticated(player)) {
            com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon type = event.getPacketType();

            if (isRestrictedPacket(type)) {
                event.setCancelled(true);
            }
        }
    }

    private boolean isRestrictedPacket(com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon type) {
        // Cancel movement
        if (type.equals(PacketType.Play.Client.PLAYER_FLYING) ||
            type.equals(PacketType.Play.Client.PLAYER_POSITION) ||
            type.equals(PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) ||
            type.equals(PacketType.Play.Client.PLAYER_ROTATION)) {
            return true;
        }

        // Cancel interactions
        if (type.equals(PacketType.Play.Client.CLICK_WINDOW) ||
            type.equals(PacketType.Play.Client.INTERACT_ENTITY) ||
            type.equals(PacketType.Play.Client.PLAYER_DIGGING) ||
            type.equals(PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) ||
            type.equals(PacketType.Play.Client.ANIMATION)) {
            return true;
        }

        return false;
    }
}
