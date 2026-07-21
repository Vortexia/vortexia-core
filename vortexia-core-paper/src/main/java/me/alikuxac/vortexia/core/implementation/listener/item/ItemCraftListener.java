// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.implementation.listener.item;

import me.alikuxac.vortexia.api.VortexiaKeys;
import me.alikuxac.vortexia.core.VortexiaCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ItemCraftListener implements Listener {

    @SuppressWarnings("unused")
    private final VortexiaCore plugin;

    public ItemCraftListener(VortexiaCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType().isAir()) {
            return;
        }

        Material material = result.getType();
        if (!isToolWeaponOrArmor(material)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();

        ItemMeta meta = result.getItemMeta();
        if (meta == null) {
            return;
        }

        // Only set owner if not already set (to prevent overriding custom/pre-set ownership)
        if (meta.getPersistentDataContainer().has(VortexiaKeys.OWNER_UUID, PersistentDataType.STRING)) {
            return;
        }

        // Set owner UUID and name in PDC
        meta.getPersistentDataContainer().set(VortexiaKeys.OWNER_UUID, PersistentDataType.STRING, player.getUniqueId().toString());
        meta.getPersistentDataContainer().set(VortexiaKeys.OWNER_NAME, PersistentDataType.STRING, player.getName());

        // Display in English as requested
        List<Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
        if (lore == null) {
            lore = new ArrayList<>();
        }

        // Add a line break if lore is not empty
        if (!lore.isEmpty()) {
            lore.add(Component.empty());
        }

        lore.add(Component.text("👤 Owner: ", NamedTextColor.GRAY)
                .append(Component.text(player.getName(), NamedTextColor.GOLD))
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        result.setItemMeta(meta);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack first = event.getInventory().getFirstItem();
        ItemStack result = event.getResult();
        if (first != null && result != null) {
            transferOwner(first, result);
            event.setResult(result);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        ItemStack base = event.getInventory().getItem(0); 
        ItemStack result = event.getResult();
        if (base != null && result != null) {
            transferOwner(base, result);
            event.setResult(result);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPrepareGrindstone(PrepareGrindstoneEvent event) {
        ItemStack upper = event.getInventory().getItem(0);
        ItemStack lower = event.getInventory().getItem(1);
        ItemStack result = event.getResult();
        if (result != null) {
            if (upper != null && upper.hasItemMeta() && upper.getItemMeta().getPersistentDataContainer().has(VortexiaKeys.OWNER_UUID, PersistentDataType.STRING)) {
                transferOwner(upper, result);
            } else if (lower != null && lower.hasItemMeta() && lower.getItemMeta().getPersistentDataContainer().has(VortexiaKeys.OWNER_UUID, PersistentDataType.STRING)) {
                transferOwner(lower, result);
            }
            event.setResult(result);
        }
    }

    private void transferOwner(ItemStack source, ItemStack target) {
        if (source == null || source.getType().isAir() || target == null || target.getType().isAir()) {
            return;
        }

        ItemMeta sourceMeta = source.getItemMeta();
        if (sourceMeta == null) return;

        String ownerUuid = sourceMeta.getPersistentDataContainer().get(VortexiaKeys.OWNER_UUID, PersistentDataType.STRING);
        String ownerName = sourceMeta.getPersistentDataContainer().get(VortexiaKeys.OWNER_NAME, PersistentDataType.STRING);

        if (ownerUuid == null) return;

        ItemMeta targetMeta = target.getItemMeta();
        if (targetMeta == null) return;

        // Set PDC
        targetMeta.getPersistentDataContainer().set(VortexiaKeys.OWNER_UUID, PersistentDataType.STRING, ownerUuid);
        if (ownerName != null) {
            targetMeta.getPersistentDataContainer().set(VortexiaKeys.OWNER_NAME, PersistentDataType.STRING, ownerName);
        } else {
            ownerName = "Unknown";
        }

        // Update Lore
        List<Component> lore = targetMeta.hasLore() ? targetMeta.lore() : new ArrayList<>();
        if (lore == null) lore = new ArrayList<>();

        // Remove existing owner lore lines to avoid duplicates
        lore.removeIf(line -> {
            String plainText = PlainTextComponentSerializer.plainText().serialize(line);
            return plainText.contains("Owner:");
        });

        // Add a line break if lore is not empty and last element is not empty
        if (!lore.isEmpty() && !lore.get(lore.size() - 1).equals(Component.empty())) {
            lore.add(Component.empty());
        }

        lore.add(Component.text("👤 Owner: ", NamedTextColor.GRAY)
                .append(Component.text(ownerName, NamedTextColor.GOLD))
                .decoration(TextDecoration.ITALIC, false));

        targetMeta.lore(lore);
        target.setItemMeta(targetMeta);
    }

    private boolean isToolWeaponOrArmor(Material material) {
        String name = material.name();
        return name.endsWith("_HELMET") ||
               name.endsWith("_CHESTPLATE") ||
               name.endsWith("_LEGGINGS") ||
               name.endsWith("_BOOTS") ||
               name.endsWith("_SWORD") ||
               name.endsWith("_PICKAXE") ||
               name.endsWith("_AXE") ||
               name.endsWith("_SHOVEL") ||
               name.endsWith("_HOE") ||
               name.equals("BOW") ||
               name.equals("CROSSBOW") ||
               name.equals("TRIDENT") ||
               name.equals("MACE") ||
               name.equals("SHIELD") ||
               name.equals("SHEARS") ||
               name.equals("FISHING_ROD") ||
               name.equals("FLINT_AND_STEEL") ||
               name.equals("BRUSH") ||
               name.equals("ELYTRA");
    }
}
