// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.core.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.alikuxac.vortexia.api.network.protocol.MachineSyncPacket;
import me.alikuxac.vortexia.api.network.protocol.NetworkChannels;
import me.alikuxac.vortexia.core.VortexiaCore;
import me.alikuxac.vortexia.core.implementation.network.MachineSyncPacketListener;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * Manages plugin messaging channels and outgoing/incoming binary packet broadcasts.
 */
public class NetworkSyncManager {

    private final VortexiaCore plugin;
    private final MachineSyncPacketListener packetListener;

    public NetworkSyncManager(VortexiaCore plugin) {
        this.plugin = plugin;
        this.packetListener = new MachineSyncPacketListener(plugin);
    }

    /**
     * Registers incoming and outgoing channels for binary payload synchronization.
     */
    public void registerChannels() {
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, NetworkChannels.SYNC_CHANNEL, packetListener);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, NetworkChannels.HUD_CHANNEL, packetListener);

        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, NetworkChannels.SYNC_CHANNEL);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, NetworkChannels.HUD_CHANNEL);

        plugin.getLoggerService().info("Registered binary network sync channels: " + NetworkChannels.SYNC_CHANNEL + ", " + NetworkChannels.HUD_CHANNEL);
    }

    /**
     * Unregisters registered plugin channels on shutdown.
     */
    public void unregisterChannels() {
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, NetworkChannels.SYNC_CHANNEL);
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, NetworkChannels.HUD_CHANNEL);

        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, NetworkChannels.SYNC_CHANNEL);
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, NetworkChannels.HUD_CHANNEL);
    }

    /**
     * Sends a MachineSyncPacket to a target player via a given channel.
     */
    public void sendMachineSync(Player player, String channel, MachineSyncPacket packet) {
        Objects.requireNonNull(player, "Player cannot be null");
        Objects.requireNonNull(packet, "Packet cannot be null");

        ByteBuf buf = Unpooled.buffer(15);
        try {
            packet.writeTo(buf);
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            player.sendPluginMessage(plugin, channel, data);
        } finally {
            buf.release();
        }
    }

    /**
     * Broadcasts a MachineSyncPacket to all players near a location.
     */
    public void broadcastMachineSync(Location location, double radius, String channel, MachineSyncPacket packet) {
        Objects.requireNonNull(location, "Location cannot be null");
        if (location.getWorld() == null) return;

        double radiusSquared = radius * radius;
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(location) <= radiusSquared) {
                sendMachineSync(player, channel, packet);
            }
        }
    }

    public MachineSyncPacketListener getPacketListener() {
        return packetListener;
    }
}
