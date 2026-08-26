// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.implementation.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.alikuxac.vortexia.api.network.protocol.MachineSyncPacket;
import me.alikuxac.vortexia.api.network.protocol.NetworkChannels;
import me.alikuxac.vortexia.core.VortexiaCore;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

/**
 * Handles incoming binary packets for machine synchronization and HUD metrics.
 */
public class MachineSyncPacketListener implements PluginMessageListener {

    private final VortexiaCore plugin;

    public MachineSyncPacketListener(VortexiaCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (message == null || message.length == 0) {
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

            // Process packet logic here (e.g. notify HUD components or update machine states)
        } catch (Exception e) {
            plugin.getLoggerService().warn("Failed to deserialize MachineSyncPacket from " + player.getName() + ": " + e.getMessage());
        } finally {
            buf.release();
        }
    }
}
