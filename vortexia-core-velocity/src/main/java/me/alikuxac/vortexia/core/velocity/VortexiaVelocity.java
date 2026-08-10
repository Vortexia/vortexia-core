// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.alikuxac.vortexia.core.proxy.ProxyAuthManager;
import me.alikuxac.vortexia.core.proxy.ProxyMessageDecoder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

@Plugin(
        id = "vortexiacore",
        name = "VortexiaCore",
        version = "${version}",
        description = "Velocity support for Vortexia core"
)
public class VortexiaVelocity {

    private static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from(ProxyAuthManager.getChannel());
    private final ProxyServer server;
    private final Logger logger;
    private final ProxyAuthManager authManager;

    @Inject
    public VortexiaVelocity(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
        this.authManager = new ProxyAuthManager();
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        server.getChannelRegistrar().register(CHANNEL);
        logger.info("VortexiaCore proxy support (Velocity) enabled successfully!");
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        authManager.unauthenticate(event.getPlayer().getUniqueId());
    }

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        Player player = event.getPlayer();
        if (player.getCurrentServer().isEmpty()) {
            return;
        }

        if (!authManager.isAuthenticated(player.getUniqueId())) {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            player.sendMessage(Component.text("You must authenticate your PIN before switching servers!", NamedTextColor.RED));
        }
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(CHANNEL)) {
            return;
        }

        ProxyMessageDecoder.parseAuthSync(event.getData()).ifPresent(payload -> {
            authManager.authenticate(payload.uuid());
            logger.info("Player authenticated on proxy: " + payload.uuid());

            for (RegisteredServer targetServer : server.getAllServers()) {
                targetServer.sendPluginMessage(CHANNEL, event.getData());
            }
        });
    }
}
