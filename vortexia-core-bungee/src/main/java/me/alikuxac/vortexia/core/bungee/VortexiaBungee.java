// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.bungee;

import me.alikuxac.vortexia.core.proxy.ProxyAuthManager;
import me.alikuxac.vortexia.core.proxy.ProxyMessageDecoder;
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

public class VortexiaBungee extends Plugin implements Listener {

    public static final String CHANNEL = ProxyAuthManager.getChannel();
    private final ProxyAuthManager authManager = new ProxyAuthManager();

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
        authManager.unauthenticate(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onServerConnect(ServerConnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        if (player.getServer() == null) {
            return;
        }

        if (!authManager.isAuthenticated(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(new TextComponent(ChatColor.RED + "You must authenticate your PIN before switching servers!"));
        }
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getTag().equals(CHANNEL)) {
            return;
        }

        ProxyMessageDecoder.parseAuthSync(event.getData()).ifPresent(payload -> {
            authManager.authenticate(payload.uuid());
            getLogger().info("Player authenticated on proxy: " + payload.uuid());

            for (ServerInfo serverInfo : getProxy().getServers().values()) {
                serverInfo.sendData(CHANNEL, event.getData());
            }
        });
    }
}
