// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.implementation.gui;

import me.alikuxac.vortexia.api.VortexiaKeys;
import me.alikuxac.vortexia.api.addon.VortexiaAddon;
import me.alikuxac.vortexia.api.item.VortexiaItem;
import me.alikuxac.vortexia.core.VortexiaCore;
import me.alikuxac.vortexia.core.core.permission.Permission;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class GuideGUI implements Listener {

    private final VortexiaCore plugin;
    private final RecipeViewerGUI recipeViewer;

    public GuideGUI(VortexiaCore plugin) {
        this.plugin = plugin;
        this.recipeViewer = new RecipeViewerGUI(plugin, this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public static class MainGuideHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public static class CategoryGuideHolder implements InventoryHolder {
        private final String addonId;

        public CategoryGuideHolder(String addonId) {
            this.addonId = addonId;
        }

        public String getAddonId() {
            return addonId;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public void openMain(Player player) {
        Inventory inv = Bukkit.createInventory(new MainGuideHolder(), 54, Component.text("Vortexia - Guide Book", NamedTextColor.AQUA, TextDecoration.BOLD));

        // Glass fillers
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        if (fm != null) {
            fm.displayName(Component.text(" "));
            filler.setItemMeta(fm);
        }
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Active addons as categories
        Collection<VortexiaAddon> addons = plugin.getAddonManager().getAddons();
        int[] categorySlots = { 20, 21, 22, 23, 24, 29, 30, 31, 32, 33 }; // Layout slots

        int slotIndex = 0;
        for (VortexiaAddon addon : addons) {
            if (slotIndex >= categorySlots.length) break;

            Material mat = getAddonIcon(addon.getAddonName());
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(addon.getAddonName() + " Addon", NamedTextColor.YELLOW, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("Version: " + addon.getVersion(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Author: " + addon.getAuthor(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text(""));
                lore.add(Component.text("▶ Click to open category", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
                
                // Standard custom tag for matching
                meta.getPersistentDataContainer().set(new NamespacedKey("vortexia", "addon_category"), PersistentDataType.STRING, addon.getAddonName());
                meta.lore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(categorySlots[slotIndex++], item);
        }

        player.openInventory(inv);
    }

    public void openCategory(Player player, String addonId) {
        String title = "Items: " + addonId.toUpperCase();
        Inventory inv = Bukkit.createInventory(new CategoryGuideHolder(addonId), 54, Component.text(title, NamedTextColor.GOLD));

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        if (fm != null) {
            fm.displayName(Component.text(" "));
            filler.setItemMeta(fm);
        }
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Fetch registered items
        Collection<VortexiaItem> items = plugin.getItemRegistry().getItemsByAddon(addonId);
        int[] itemSlots = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        };

        int index = 0;
        for (VortexiaItem vItem : items) {
            if (index >= itemSlots.length) break;

            ItemStack showItem = vItem.getItemStack().clone();
            ItemMeta meta = showItem.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.lore();
                if (lore == null) lore = new ArrayList<>();
                if (meta.getPersistentDataContainer().has(VortexiaKeys.VORTEXIA_ITEM, PersistentDataType.INTEGER) || meta.getPersistentDataContainer().has(VortexiaKeys.ITEM_ID, PersistentDataType.STRING)) {
                    lore.add(Component.text(""));
                    lore.add(Component.text("§8▶ Addon: §a" + vItem.getAddonId()));
                }
                lore.add(Component.text(""));
                
                // Creative warning/give instruction
                if (player.hasPermission(Permission.CREATIVE_GIVE.getNode()) || player.getGameMode() == GameMode.CREATIVE) {
                    lore.add(Component.text("◀ Left-Click to get item (Creative)", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
                }
                lore.add(Component.text("▶ Right-Click to see recipe", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
                showItem.setItemMeta(meta);
            }

            inv.setItem(itemSlots[index++], showItem);
        }

        // Back to Main screen button at 49
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta();
        if (bm != null) {
            bm.displayName(Component.text("Back", NamedTextColor.RED));
            back.setItemMeta(bm);
        }
        inv.setItem(49, back);

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof MainGuideHolder) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() == Material.AIR || item.getItemMeta() == null) return;

            String addon = item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey("vortexia", "addon_category"), PersistentDataType.STRING);
            if (addon != null) {
                openCategory((Player) event.getWhoClicked(), addon);
            }
            return;
        }

        if (holder instanceof CategoryGuideHolder catHolder) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            int slot = event.getRawSlot();

            if (slot == 49) {
                openMain(player);
                return;
            }

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR || clicked.getItemMeta() == null) return;
            if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

            String itemId = clicked.getItemMeta().getPersistentDataContainer().get(VortexiaKeys.ITEM_ID, PersistentDataType.STRING);
            if (itemId == null) return;

            if ("cell".equals(itemId)) {
                Integer cap = clicked.getItemMeta().getPersistentDataContainer().get(new NamespacedKey("vortexia", "cell_capacity"), PersistentDataType.INTEGER);
                if (cap != null) {
                    itemId = switch(cap) {
                        case 1000 -> "cell_copper";
                        case 4000 -> "cell_iron";
                        case 16000 -> "cell_gold";
                        case 64000 -> "cell_diamond";
                        case 256000 -> "cell_netherite";
                        default -> "cell";
                    };
                }
            }

            // Fetch original item to preserve lore
            Optional<VortexiaItem> oItem = plugin.getItemRegistry().getItem(catHolder.getAddonId() + ":" + itemId);
            if (oItem.isEmpty()) return;
            VortexiaItem originalItem = oItem.get();

            if (event.isLeftClick() && (player.hasPermission(Permission.CREATIVE_GIVE.getNode()) || player.getGameMode() == GameMode.CREATIVE)) {
                player.getInventory().addItem(originalItem.getItemStack().clone());
                
                Component displayName = null;
                if (originalItem.getItemStack().getItemMeta() != null && originalItem.getItemStack().getItemMeta().hasDisplayName()) {
                    displayName = originalItem.getItemStack().getItemMeta().displayName();
                }
                
                if (displayName == null) {
                    displayName = Component.text(originalItem.getItemId());
                }
                
                player.sendMessage(Component.text("Received 1x ", NamedTextColor.GREEN).append(displayName));
            } else if (event.isRightClick()) {
                recipeViewer.open(player, originalItem);
            }
        }
    }

    private Material getAddonIcon(String name) {
        if (name == null) return Material.BOOK;
        return switch (name.toLowerCase()) {
            case "storage" -> Material.ENDER_CHEST;
            case "kinetic" -> Material.COPPER_BLOCK;
            case "industrial" -> Material.ANVIL;
            case "vitality" -> Material.GOLDEN_APPLE;
            default -> Material.BOOK;
        };
    }
}
