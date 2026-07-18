// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.implementation.gui;

import me.alikuxac.vortexia.api.VortexiaKeys;
import me.alikuxac.vortexia.api.item.VortexiaItem;
import me.alikuxac.vortexia.core.VortexiaCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class RecipeViewerGUI implements Listener {

    private final VortexiaCore plugin;
    private final GuideGUI guideGUI;

    public RecipeViewerGUI(VortexiaCore plugin, GuideGUI guideGUI) {
        this.plugin = plugin;
        this.guideGUI = guideGUI;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public static class RecipeHolder implements InventoryHolder {
        private final String addonId;

        public RecipeHolder(String addonId) {
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
    public void open(Player player, VortexiaItem item) {
        Component titleComponent = Component.text("Recipe: ", NamedTextColor.GOLD);
        if (item.getItemStack().getItemMeta() != null && item.getItemStack().getItemMeta().hasDisplayName()) {
            titleComponent = titleComponent.append(item.getItemStack().getItemMeta().displayName());
        } else {
            titleComponent = titleComponent.append(Component.text(item.getItemId()));
        }

        Inventory inv = Bukkit.createInventory(new RecipeHolder(item.getAddonId()), 54, titleComponent);

        // Background fillers
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        if (fm != null) {
            fm.displayName(Component.text(" "));
            filler.setItemMeta(fm);
        }

        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // 3x3 Grid mappings
        int[] slots = {
            10, 11, 12,
            19, 20, 21,
            28, 29, 30
        };

        ItemStack[] recipe = item.getRecipe();
        if (recipe != null) {
            for (int i = 0; i < Math.min(slots.length, recipe.length); i++) {
                if (recipe[i] != null) {
                    inv.setItem(slots[i], recipe[i]);
                } else {
                    inv.setItem(slots[i], new ItemStack(Material.AIR));
                }
            }
        } else {
            // Uncraftable item message in center
            ItemStack uncraftable = new ItemStack(Material.BARRIER);
            ItemMeta um = uncraftable.getItemMeta();
            if (um != null) {
                um.displayName(Component.text("Can't Craft", NamedTextColor.RED));
                uncraftable.setItemMeta(um);
            }
            inv.setItem(20, uncraftable);
        }

        // Crafting Table Icon in 23
        ItemStack table = new ItemStack(Material.CRAFTING_TABLE);
        ItemMeta tm = table.getItemMeta();
        if (tm != null) {
            tm.displayName(Component.text("Craft at Crafting Table", NamedTextColor.YELLOW));
            table.setItemMeta(tm);
        }
        inv.setItem(23, table);

        // Result Item in 24
        ItemStack resultStack = item.getItemStack().clone();
        ItemMeta rm = resultStack.getItemMeta();
        if (rm != null) {
            List<Component> lore = rm.lore();
            if (lore == null) lore = new ArrayList<>();
            if (rm.getPersistentDataContainer().has(VortexiaKeys.VORTEXIA_ITEM, PersistentDataType.INTEGER) || rm.getPersistentDataContainer().has(VortexiaKeys.ITEM_ID, PersistentDataType.STRING)) {
                lore.add(Component.text(""));
                lore.add(Component.text("§8▶ Addon: §a" + item.getAddonId()));
            }
            rm.lore(lore);
            resultStack.setItemMeta(rm);
        }
        inv.setItem(24, resultStack);

        // Back Button in 49
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
        if (!(event.getInventory().getHolder() instanceof RecipeHolder holder)) return;
        event.setCancelled(true);

        if (event.getRawSlot() == 49) {
            Player player = (Player) event.getWhoClicked();
            guideGUI.openCategory(player, holder.getAddonId());
        }
    }
}
