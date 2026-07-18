// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.implementation.item;

import me.alikuxac.vortexia.api.VortexiaKeys;
import me.alikuxac.vortexia.api.item.ItemRegistry;
import me.alikuxac.vortexia.api.item.VortexiaItem;
import me.alikuxac.vortexia.core.VortexiaCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CoreItemRegistry implements ItemRegistry {

    private final VortexiaCore plugin;
    private final Map<String, VortexiaItem> items = new ConcurrentHashMap<>();

    public CoreItemRegistry(VortexiaCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void registerItem(VortexiaItem item) {
        String key = (item.getAddonId() + ":" + item.getItemId()).toLowerCase();
        if (items.containsKey(key)) {
            plugin.getLogger().warning("Custom item already registered: " + key);
            return;
        }

        // Automatically style the ItemStack with the Addon ID as its blue italicized mod source (e.g., "Storage")
        ItemStack itemStack = item.getItemStack();
        if (itemStack != null && itemStack.hasItemMeta()) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
                if (lore == null) lore = new ArrayList<>();
                
                if (meta.getPersistentDataContainer().has(VortexiaKeys.VORTEXIA_ITEM, PersistentDataType.INTEGER) || meta.getPersistentDataContainer().has(VortexiaKeys.ITEM_ID, PersistentDataType.STRING)) {
                    // Add an empty line if there's already description lore
                    if (!lore.isEmpty()) {
                        lore.add(Component.empty());
                    }
                    
                    // Add the italic blue source name (e.g. "Storage")
                    lore.add(Component.text(item.getAddonId(), NamedTextColor.BLUE)
                            .decoration(TextDecoration.ITALIC, true));
                }
                
                meta.lore(lore);
                itemStack.setItemMeta(meta);
            }
        }

        items.put(key, item);
        plugin.getLogger().info("Registered custom item: " + key);
    }

    @Override
    public Optional<VortexiaItem> getItem(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(items.get(id.toLowerCase()));
    }

    @Override
    public Collection<VortexiaItem> getItems() {
        return Collections.unmodifiableCollection(items.values());
    }

    @Override
    public Collection<VortexiaItem> getItemsByAddon(String addonId) {
        if (addonId == null) return Collections.emptyList();
        String searchId = addonId.toLowerCase();
        return items.values().stream()
                .filter(item -> item.getAddonId().toLowerCase().equals(searchId))
                .collect(Collectors.toList());
    }
}
