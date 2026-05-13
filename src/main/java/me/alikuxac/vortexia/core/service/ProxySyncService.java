// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.service;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.alikuxac.vortexia.core.VortexiaCore;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.UUID;

public class ProxySyncService implements PluginMessageListener {

    public static final String CHANNEL = "vortexia:main";
    private final VortexiaCore plugin;

    public ProxySyncService(VortexiaCore plugin) {
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
    }

    public void broadcastAuthentication(Player player) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("AUTH_SYNC");
        out.writeUTF(player.getUniqueId().toString());
        
        player.sendPluginMessage(plugin, CHANNEL, out.toByteArray());
        plugin.getLoggerService().debug("Broadcasted AUTH_SYNC for " + player.getName());
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(CHANNEL)) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();

        if (subChannel.equals("AUTH_SYNC")) {
            UUID uuid = UUID.fromString(in.readUTF());
            plugin.getSecurityManager().authenticateLocally(uuid);
            plugin.getLoggerService().debug("Received AUTH_SYNC for UUID: " + uuid);
        }
    }
}
