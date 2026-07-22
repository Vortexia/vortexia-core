// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Plugin(
        id = "vortexiacore",
        name = "VortexiaCore",
        version = "${version}",
        description = "Velocity support for Vortexia core"
)
public class VortexiaVelocity {

    private static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from("vortexia:main");
    private final ProxyServer server;
    private final Logger logger;
    private final Set<UUID> authenticatedPlayers = ConcurrentHashMap.newKeySet();

    @Inject
    public VortexiaVelocity(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        server.getChannelRegistrar().register(CHANNEL);
        logger.info("VortexiaCore proxy support (Velocity) enabled successfully!");
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        authenticatedPlayers.remove(event.getPlayer().getUniqueId());
    }

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        Player player = event.getPlayer();
        // Allow the initial connection to a backend server
        if (player.getCurrentServer().isEmpty()) {
            return;
        }

        if (!authenticatedPlayers.contains(player.getUniqueId())) {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            player.sendMessage(Component.text("You must authenticate your PIN before switching servers!", NamedTextColor.RED));
        }
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(CHANNEL)) {
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
                logger.info("Player authenticated on proxy: " + uuid);

                // Broadcast this auth event to all other registered backend servers
                for (RegisteredServer targetServer : server.getAllServers()) {
                    targetServer.sendPluginMessage(CHANNEL, event.getData());
                }
            }
        } catch (Exception e) {
            logger.error("Failed to parse plugin message on Velocity: " + e.getMessage());
        }
    }
}
