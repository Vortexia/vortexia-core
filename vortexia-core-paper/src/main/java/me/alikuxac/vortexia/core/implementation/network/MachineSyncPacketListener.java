// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.implementation.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.alikuxac.vortexia.api.network.protocol.MachineSyncPacket;
import me.alikuxac.vortexia.api.network.protocol.NetworkChannels;
import me.alikuxac.vortexia.core.VortexiaCore;
import me.alikuxac.vortexia.core.core.network.NetworkSyncManager;
import me.alikuxac.vortexia.core.core.network.SubscriptionManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

/**
 * Handles incoming binary packets for machine synchronization, subscription, and HUD metrics.
 */
public class MachineSyncPacketListener implements PluginMessageListener {

    private final VortexiaCore plugin;
    private final SubscriptionManager subscriptionManager;

    public MachineSyncPacketListener(VortexiaCore plugin, SubscriptionManager subscriptionManager) {
        this.plugin = plugin;
        this.subscriptionManager = subscriptionManager;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (message == null || message.length == 0) {
            return;
        }

        if (NetworkSyncManager.SUBSCRIBE_CHANNEL.equals(channel)) {
            handleSubscribe(player, message);
            return;
        } else if (NetworkSyncManager.UNSUBSCRIBE_CHANNEL.equals(channel)) {
            handleUnsubscribe(player, message);
            return;
        }

        if (!NetworkChannels.SYNC_CHANNEL.equals(channel) && !NetworkChannels.HUD_CHANNEL.equals(channel)) {
            return;
        }

        ByteBuf buf = Unpooled.wrappedBuffer(message);
        try {
            if (buf.readableBytes() < 15) {
                plugin.getLoggerService().warn("Received malformed binary packet on channel " + channel + " (less than 15 bytes)");
                return;
            }

            MachineSyncPacket packet = MachineSyncPacket.readFrom(buf);
            plugin.getLoggerService().debug(String.format(
                    "Received packet on [%s] from %s -> BlockPos: %d, MachineID: %d, Energy: %d, State: %d",
                    channel, player.getName(), packet.blockPos(), packet.machineId(), packet.currentEnergy(), packet.state()
            ));

            // Process packet logic here
        } catch (Exception e) {
            plugin.getLoggerService().warn("Failed to deserialize MachineSyncPacket from " + player.getName() + ": " + e.getMessage());
        } finally {
            buf.release();
        }
    }

    private void handleSubscribe(Player player, byte[] message) {
        ByteBuf buf = Unpooled.wrappedBuffer(message);
        try {
            if (buf.readableBytes() >= 8) {
                long blockPos = buf.readLong();
                subscriptionManager.subscribe(player, blockPos);
                plugin.getLoggerService().debug("Player " + player.getName() + " subscribed to blockPos: " + blockPos);
            }
        } catch (Exception e) {
            plugin.getLoggerService().warn("Failed to parse subscribe packet from " + player.getName() + ": " + e.getMessage());
        } finally {
            buf.release();
        }
    }

    private void handleUnsubscribe(Player player, byte[] message) {
        ByteBuf buf = Unpooled.wrappedBuffer(message);
        try {
            if (buf.readableBytes() >= 8) {
                long blockPos = buf.readLong();
                subscriptionManager.unsubscribe(player, blockPos);
                plugin.getLoggerService().debug("Player " + player.getName() + " unsubscribed from blockPos: " + blockPos);
            } else {
                subscriptionManager.unsubscribeAll(player);
                plugin.getLoggerService().debug("Player " + player.getName() + " unsubscribed from all block positions");
            }
        } catch (Exception e) {
            plugin.getLoggerService().warn("Failed to parse unsubscribe packet from " + player.getName() + ": " + e.getMessage());
        } finally {
            buf.release();
        }
    }
}

