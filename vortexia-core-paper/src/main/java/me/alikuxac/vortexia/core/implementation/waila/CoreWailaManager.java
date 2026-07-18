// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.implementation.waila;

import me.alikuxac.vortexia.api.waila.WailaManager;
import me.alikuxac.vortexia.api.waila.WailaProvider;
import me.alikuxac.vortexia.api.network.wireless.WirelessNode;
import me.alikuxac.vortexia.api.network.wireless.WirelessChannelInfo;
import me.alikuxac.vortexia.core.VortexiaCore;
import me.alikuxac.vortexia.core.core.network.wireless.CoreWirelessNetworkRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class CoreWailaManager implements WailaManager {

    private final List<WailaProvider> providers = new CopyOnWriteArrayList<>();

    public CoreWailaManager() {
        // Register default Vanilla Block Provider first to guarantee priority
        registerProvider(this::getVanillaWailaInfo);
        
        // Register default Wireless Node Provider second
        registerProvider(this::getWirelessWailaInfo);
    }

    @Override
    public void registerProvider(WailaProvider provider) {
        if (provider != null && !providers.contains(provider)) {
            providers.add(provider);
        }
    }

    @Override
    public void unregisterProvider(WailaProvider provider) {
        if (provider != null) {
            providers.remove(provider);
        }
    }

    @Override
    public List<Component> getInfo(Player player, Block block) {
        List<Component> components = new ArrayList<>();
        for (WailaProvider provider : providers) {
            try {
                List<Component> info = provider.getWailaInfo(player, block);
                if (info != null && !info.isEmpty()) {
                    components.addAll(info);
                }
            } catch (Exception e) {
                VortexiaCore.getInstance().getLogger().warning("Error in WailaProvider: " + e.getMessage());
            }
        }
        return components;
    }

    /**
     * Default provider for vanilla Minecraft blocks.
     */
    private List<Component> getVanillaWailaInfo(Player player, Block block) {
        List<Component> info = new ArrayList<>();
        if (block == null || block.getType().isAir()) {
            return info;
        }

        String rawName = block.getType().name();
        String formattedName = formatBlockName(rawName);
        String namespace = "minecraft:" + rawName.toLowerCase();

        // 1. Dòng chính: Tên khối & Namespace
        info.add(MiniMessage.miniMessage().deserialize(
            "<white><bold>" + formattedName + "</bold></white> <dark_gray>|</dark_gray> <gray>" + namespace + "</gray>"
        ));

        // 2. Secondary line: Container info (Chest, Barrel...)
        try {
            if (block.getState() instanceof Container container) {
                int size = container.getInventory().getSize();
                int filled = 0;
                for (ItemStack item : container.getInventory().getContents()) {
                    if (item != null && !item.getType().isAir()) {
                        filled++;
                    }
                }
                info.add(MiniMessage.miniMessage().deserialize(
                    "<gold>📦 Slots:</gold> <yellow>" + filled + "</yellow><gray>/</gray><yellow>" + size + "</yellow>"
                ));
            } else if (block.getState() instanceof Furnace furnace) {
                if (furnace.getBurnTime() > 0) {
                    info.add(MiniMessage.miniMessage().deserialize(
                        "<red>🔥 Active cooking</red>"
                    ));
                } else {
                    info.add(MiniMessage.miniMessage().deserialize(
                        "<gray>❄ Idle</gray>"
                    ));
                }
            }
        } catch (Exception ignored) {}

        // 3. Tool requirement information line
        String toolType = detectRequiredTool(rawName);
        if (!toolType.equals("None")) {
            ItemStack handItem = player.getInventory().getItemInMainHand();
            boolean isPreferred = block.isPreferredTool(handItem);
            String statusSymbol = isPreferred ? "<green>✔</green>" : "<red>✘</red>";
            String toolIcon = getToolIcon(toolType);

            info.add(MiniMessage.miniMessage().deserialize(
                "<gray>Tool: " + toolIcon + " " + toolType + " " + statusSymbol + "</gray>"
            ));
        }

        return info;
    }

    /**
     * Default provider for Wireless/Virtual Network Nodes.
     */
    private List<Component> getWirelessWailaInfo(Player player, Block block) {
        List<Component> info = new ArrayList<>();
        if (block == null) return info;

        if (VortexiaCore.getInstance().getWirelessRegistry() instanceof CoreWirelessNetworkRegistry registry) {
            WirelessNode node = registry.getWirelessNodeAt(block.getLocation());
            if (node != null) {
                info.add(MiniMessage.miniMessage().deserialize(
                    "<aqua>📶 Freq:</aqua> <white>" + node.getFrequency() + "</white> <dark_gray>|</dark_gray> <yellow>ROLE: " + node.getRole().name() + "</yellow>"
                ));
                
                Optional<WirelessChannelInfo> channelOpt = registry.getChannelInfo(node.getFrequency());
                if (channelOpt.isPresent()) {
                    WirelessChannelInfo channel = channelOpt.get();
                    info.add(MiniMessage.miniMessage().deserialize(
                        "<aqua>🚀 Limit:</aqua> <green>" + channel.getMaxThroughput() + " SU</green> <dark_gray>(</dark_gray><gray>x" + channel.getSpeedMultiplier() + " spd</gray><dark_gray>)</dark_gray>"
                    ));
                }
            }
        }
        return info;
    }

    private String formatBlockName(String name) {
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

    private String detectRequiredTool(String rawName) {
        if (rawName.contains("ORE") || rawName.contains("STONE") || rawName.contains("DEEPSLATE") || 
            rawName.contains("GRANITE") || rawName.contains("DIORITE") || rawName.contains("ANDESITE") || 
            rawName.contains("BRICK") || rawName.contains("TERRACOTTA") || rawName.contains("CONCRETE") ||
            rawName.contains("IRON") || rawName.contains("GOLD") || rawName.contains("COPPER") || rawName.contains("OBSIDIAN")) {
            return "Pickaxe";
        }
        if (rawName.contains("LOG") || rawName.contains("WOOD") || rawName.contains("PLANKS") || 
            rawName.contains("CHEST") || rawName.contains("DOOR") || rawName.contains("FENCE") || rawName.contains("SIGN")) {
            return "Axe";
        }
        if (rawName.contains("DIRT") || rawName.contains("GRASS") || rawName.contains("SAND") || 
            rawName.contains("GRAVEL") || rawName.contains("CLAY") || rawName.contains("SNOW")) {
            return "Shovel";
        }
        return "None";
    }

    private String getToolIcon(String toolType) {
        return switch (toolType) {
            case "Pickaxe" -> "⛏";
            case "Axe" -> "🪓";
            case "Shovel" -> "🧹";
            default -> "";
        };
    }
}
