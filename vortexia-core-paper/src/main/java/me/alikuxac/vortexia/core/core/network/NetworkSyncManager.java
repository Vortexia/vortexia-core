// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.core.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.alikuxac.vortexia.api.network.protocol.MachineSyncPacket;
import me.alikuxac.vortexia.api.network.protocol.NetworkChannels;
import me.alikuxac.vortexia.core.VortexiaCore;
import me.alikuxac.vortexia.core.implementation.network.MachineSyncPacketListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;

/**
 * Manages plugin messaging channels and outgoing/incoming binary packet broadcasts.
 */
public class NetworkSyncManager {

    public static final String SUBSCRIBE_CHANNEL = "vortexia:subscribe";
    public static final String UNSUBSCRIBE_CHANNEL = "vortexia:unsubscribe";

    private final VortexiaCore plugin;
    private final MachineSyncPacketListener packetListener;
    private final SubscriptionManager subscriptionManager;
    private final MachineMetricsDeltaTracker deltaTracker;

    public NetworkSyncManager(VortexiaCore plugin) {
        this.plugin = plugin;
        this.subscriptionManager = new SubscriptionManager();
        this.deltaTracker = new MachineMetricsDeltaTracker();
        this.packetListener = new MachineSyncPacketListener(plugin, this.subscriptionManager);
    }

    /**
     * Registers incoming and outgoing channels for binary payload synchronization.
     */
    public void registerChannels() {
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, NetworkChannels.SYNC_CHANNEL, packetListener);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, NetworkChannels.HUD_CHANNEL, packetListener);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, SUBSCRIBE_CHANNEL, packetListener);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, UNSUBSCRIBE_CHANNEL, packetListener);

        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, NetworkChannels.SYNC_CHANNEL);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, NetworkChannels.HUD_CHANNEL);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, SUBSCRIBE_CHANNEL);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, UNSUBSCRIBE_CHANNEL);

        plugin.getLoggerService().info("Registered binary network sync channels: " 
                + NetworkChannels.SYNC_CHANNEL + ", " 
                + NetworkChannels.HUD_CHANNEL + ", "
                + SUBSCRIBE_CHANNEL + ", "
                + UNSUBSCRIBE_CHANNEL);
    }

    /**
     * Unregisters registered plugin channels on shutdown.
     */
    public void unregisterChannels() {
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, NetworkChannels.SYNC_CHANNEL);
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, NetworkChannels.HUD_CHANNEL);
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, SUBSCRIBE_CHANNEL);
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, UNSUBSCRIBE_CHANNEL);

        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, NetworkChannels.SYNC_CHANNEL);
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, NetworkChannels.HUD_CHANNEL);
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, SUBSCRIBE_CHANNEL);
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, UNSUBSCRIBE_CHANNEL);

        subscriptionManager.clear();
        deltaTracker.clear();
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
     * Broadcasts a MachineSyncPacket to all registered subscribers for a block position.
     */
    public void broadcastToSubscribers(long blockPos, String channel, MachineSyncPacket packet) {
        Objects.requireNonNull(packet, "Packet cannot be null");
        if (!deltaTracker.shouldEmit(blockPos, packet)) {
            return;
        }

        for (UUID uuid : subscriptionManager.getSubscribers(blockPos)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                sendMachineSync(player, channel, packet);
            }
        }
    }

    /**
     * Asynchronously dispatches generated binary payloads to subscribers.
     */
    public void broadcastToSubscribersAsync(long blockPos, String channel, MachineSyncPacket packet) {
        if (plugin.getSchedulerService() != null) {
            plugin.getSchedulerService().runAsync(() -> broadcastToSubscribers(blockPos, channel, packet));
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> broadcastToSubscribers(blockPos, channel, packet));
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

    public SubscriptionManager getSubscriptionManager() {
        return subscriptionManager;
    }

    public MachineMetricsDeltaTracker getDeltaTracker() {
        return deltaTracker;
    }
}

