// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.implementation.waila;

import me.alikuxac.vortexia.core.VortexiaCore;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.time.Duration;
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

        if (!enabled) {
            // If system is disabled: clean up all displays immediately
            cleanup();
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline()) {
                removePlayerHUD(player);
                continue;
            }

            // Raycast player vision within 6 blocks
            Block block = player.getTargetBlockExact(6);
            if (block == null || block.getType().isAir()) {
                // Remove display immediately if player looks at air or too far
                removePlayerHUD(player);
                continue;
            }

            // Gather all information components from WailaManager
            List<Component> info = plugin.getWailaManager().getInfo(player, block);
            if (info.isEmpty()) {
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

            // Handle left/right alignment by padding spaces (simulated alignment)
            Component aligned = applyAlignment(joined, position);

            // Categorize and display based on 9 positions (3 main channels: top -> BossBar, bottom -> ActionBar, center -> Subtitle)
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
}
