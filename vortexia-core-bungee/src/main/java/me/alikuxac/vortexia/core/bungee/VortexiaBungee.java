// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VortexiaBungee extends Plugin implements Listener {

    public static final String CHANNEL = "vortexia:main";
    private final Set<UUID> authenticatedPlayers = ConcurrentHashMap.newKeySet();

    @Override
    public void onEnable() {
        getProxy().registerChannel(CHANNEL);
        getProxy().getPluginManager().registerListener(this, this);
        getLogger().info("VortexiaCore proxy support (BungeeCord/Waterfall) enabled successfully!");
    }

    @Override
    public void onDisable() {
        getProxy().unregisterChannel(CHANNEL);
        getLogger().info("VortexiaCore proxy support (BungeeCord/Waterfall) disabled.");
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        authenticatedPlayers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onServerConnect(ServerConnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        // Allow the initial connection to the lobby/first server
        if (player.getServer() == null) {
            return;
        }

        if (!authenticatedPlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(new TextComponent(ChatColor.RED + "You must authenticate your PIN before switching servers!"));
        }
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getTag().equals(CHANNEL)) {
            return;
        }

        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(event.getData());
            DataInputStream in = new DataInputStream(bis);
            String subChannel = in.readUTF();

            if ("AUTH_SYNC".equals(subChannel)) {
                String uuidStr = in.readUTF();
                UUID uuid = UUID.fromString(uuidStr);
                authenticatedPlayers.add(uuid);
                getLogger().info("Player authenticated on proxy: " + uuid);

                // Broadcast this auth event to all other registered backend servers
                for (ServerInfo serverInfo : getProxy().getServers().values()) {
                    serverInfo.sendData(CHANNEL, event.getData());
                }
            }
        } catch (Exception e) {
            getLogger().severe("Failed to parse plugin message on BungeeCord: " + e.getMessage());
        }
    }
}
