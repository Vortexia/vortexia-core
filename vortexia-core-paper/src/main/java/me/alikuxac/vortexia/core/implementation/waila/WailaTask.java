// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.implementation.waila;

import me.alikuxac.vortexia.api.network.protocol.MachineSyncPacket;
import me.alikuxac.vortexia.api.network.protocol.NetworkChannels;
import me.alikuxac.vortexia.core.VortexiaCore;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WailaTask implements Runnable {

    private final VortexiaCore plugin;
    private final Map<UUID, BossBar> playerBossBars = new ConcurrentHashMap<>();

    public WailaTask(VortexiaCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        // Read dynamic configuration
        boolean enabled = plugin.getConfig().getBoolean("waila.enabled", true);
        String position = plugin.getConfig().getString("waila.position", "top - middle").toLowerCase().trim();
        int maxDistance = plugin.getConfig().getInt("raycast.max-distance", 5);

        if (!enabled) {
            // If system is disabled: clean up all displays immediately
            cleanup();
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline() || (plugin.getHudManager() != null && !plugin.getHudManager().isHudEnabled(player))) {
                if (plugin.getNetworkSyncManager() != null) {
                    plugin.getNetworkSyncManager().getSubscriptionManager().unsubscribeAll(player);
                }
                removePlayerHUD(player);
                continue;
            }

            // Check if player has custom client mod installed
            boolean isModdedClient = plugin.getModDetectorService() != null && plugin.getModDetectorService().isModded(player);

            // Raycast player vision within configured max-distance blocks to detect both Blocks and Entities
            RayTraceResult rayTrace = player.getWorld().rayTrace(
                player.getEyeLocation(),
                player.getLocation().getDirection(),
                maxDistance,
                FluidCollisionMode.NEVER,
                true,
                0.2,
                entity -> entity != player && (entity instanceof LivingEntity)
            );

            List<Component> info = new ArrayList<>();
            if (rayTrace != null) {
                if (rayTrace.getHitEntity() instanceof LivingEntity living) {
                    // Populate entity information
                    Component customNameComp = living.customName();
                    Component entityNameComp = customNameComp != null
                        ? customNameComp
                        : Component.text(formatEntityName(living.getType().name()));
                    String namespace = "minecraft:" + living.getType().name().toLowerCase();
                    info.add(entityNameComp
                        .color(NamedTextColor.WHITE)
                        .decorate(TextDecoration.BOLD)
                        .append(MiniMessage.miniMessage().deserialize(
                            " <dark_gray>|</dark_gray> <gray>" + namespace + "</gray>"
                        ))
                    );
                    
                    double health = living.getHealth();
                    Attribute maxHealthAttr = null;
                    // Attribute lookup compatible with Paper 1.21.1+
                    try {
                        maxHealthAttr = Attribute.GENERIC_MAX_HEALTH;
                    } catch (Throwable t1) {
                        try {
                            maxHealthAttr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("max_health"));
                        } catch (Throwable ignored) {}
                    }

                    double maxHealth = 20.0;
                    if (maxHealthAttr != null) {
                        var instance = living.getAttribute(maxHealthAttr);
                        if (instance != null) {
                            maxHealth = instance.getValue();
                        }
                    } else {
                        @SuppressWarnings("deprecation")
                        double fallbackMax = living.getMaxHealth();
                        maxHealth = fallbackMax;
                    }
                    String formattedHealth = String.format("%.1f", health);
                    String formattedMaxHealth = String.format("%.1f", maxHealth);
                    
                    info.add(MiniMessage.miniMessage().deserialize(
                        "<red>❤ Health:</red> <yellow>" + formattedHealth + "</yellow><gray>/</gray><yellow>" + formattedMaxHealth + "</yellow>"
                    ));
                } else if (rayTrace.getHitBlock() != null && !rayTrace.getHitBlock().getType().isAir()) {
                    Block hitBlock = rayTrace.getHitBlock();
                    info = plugin.getWailaManager().getInfo(player, hitBlock);

                    if (plugin.getNetworkSyncManager() != null) {
                        try {
                            long blockPos = ((long) hitBlock.getX() & 0x3FFFFFFL) << 38 
                                          | ((long) hitBlock.getZ() & 0x3FFFFFFL) << 12 
                                          | ((long) hitBlock.getY() & 0xFFFL);
                            plugin.getNetworkSyncManager().getSubscriptionManager().updateActiveSubscription(player, blockPos);

                            short machineId = (short) (Math.abs(hitBlock.getType().hashCode()) & 0x7FFF);
                            MachineSyncPacket packet = MachineSyncPacket.of(hitBlock, machineId, 0, (byte) 1);
                            plugin.getNetworkSyncManager().sendMachineSync(player, NetworkChannels.HUD_CHANNEL, packet);
                        } catch (Exception ignored) {}
                    }
                }
            }

            if (info.isEmpty()) {
                if (plugin.getNetworkSyncManager() != null) {
                    plugin.getNetworkSyncManager().getSubscriptionManager().unsubscribeAll(player);
                }
                removePlayerHUD(player);
                continue;
            }

            // Join all info components into a single line with separator '|'
            Component joined = Component.empty();
            for (int i = 0; i < info.size(); i++) {
                joined = joined.append(info.get(i));
                if (i < info.size() - 1) {
                    joined = joined.append(Component.text("  |  ", NamedTextColor.DARK_GRAY));
                }
            }

            // Wrap in a sleek HUD container
            Component containerJoined = MiniMessage.miniMessage().deserialize("<gray>[</gray> ")
                .append(joined)
                .append(MiniMessage.miniMessage().deserialize(" <gray>]</gray>"));

            // Handle left/right alignment by padding spaces (simulated alignment)
            Component aligned = applyAlignment(containerJoined, position);

            // If player is using Modded client, network packets handle rendering - remove vanilla display
            if (isModdedClient) {
                removePlayerHUD(player);
                continue;
            }

            // Categorize and display based on 9 positions for Vanilla players
            if (position.startsWith("top")) {
                // Clear existing title and action bar if any
                player.clearTitle();
                
                UUID uuid = player.getUniqueId();
                if (playerBossBars.containsKey(uuid)) {
                    BossBar bossBar = playerBossBars.get(uuid);
                    bossBar.name(aligned);
                } else {
                    BossBar newBossBar = BossBar.bossBar(
                        aligned,
                        1.0f,
                        BossBar.Color.BLUE,
                        BossBar.Overlay.PROGRESS
                    );
                    player.showBossBar(newBossBar);
                    playerBossBars.put(uuid, newBossBar);
                }
            } else if (position.startsWith("bottom")) {
                // Renders via Action Bar. 
                // Clear existing BossBar
                removeBossBar(player.getUniqueId());
                player.clearTitle();

                player.sendActionBar(aligned);
            } else if (position.startsWith("center")) {
                // Renders via Title Subtitle
                // Clear existing BossBar
                removeBossBar(player.getUniqueId());

                Title.Times times = Title.Times.times(
                    Duration.ZERO,
                    Duration.ofMillis(350), // stay time
                    Duration.ZERO
                );
                Title title = Title.title(
                    Component.empty(), // Empty main title
                    aligned,          // Display on subtitle
                    times
                );
                player.showTitle(title);
            } else {
                // Fallback if position is invalid
                removePlayerHUD(player);
            }
        }
    }

    /**
     * Align left or right by automatically padding spaces.
     */
    private Component applyAlignment(Component base, String position) {
        String spaces = "                                                                                ";
        if (position.contains("left")) {
            return base.append(Component.text(spaces));
        } else if (position.contains("right")) {
            return Component.text(spaces).append(base);
        }
        return base;
    }

    /**
     * Hide and remove BossBar of a specific player.
     */
    private void removeBossBar(UUID uuid) {
        BossBar bossBar = playerBossBars.remove(uuid);
        if (bossBar != null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.hideBossBar(bossBar);
            }
        }
    }

    /**
     * Clean up all display channels of a specific player.
     */
    private void removePlayerHUD(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        
        // 1. Remove BossBar
        removeBossBar(uuid);
        
        // 2. Clear Title/Subtitle
        player.clearTitle();
        
        // 3. Clear Action Bar
        player.sendActionBar(Component.empty());
    }

    /**
     * Clean up all BossBars and displays of all players.
     */
    public void cleanup() {
        for (UUID uuid : playerBossBars.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                removePlayerHUD(player);
            }
        }
        playerBossBars.clear();
    }

    private String formatEntityName(String name) {
        if (name == null || name.isEmpty()) return "";
        String[] parts = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0)))
              .append(part.substring(1).toLowerCase())
              .append(" ");
        }
        return sb.toString().trim();
    }
}
