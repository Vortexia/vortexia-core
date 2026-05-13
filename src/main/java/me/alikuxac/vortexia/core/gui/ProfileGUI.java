// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.gui;

import me.alikuxac.vortexia.api.model.Identity;
import me.alikuxac.vortexia.core.VortexiaCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ProfileGUI {

    private final VortexiaCore plugin;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    public ProfileGUI(VortexiaCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer, Identity identity) {
        String title = "Citizen: " + identity.getName();
        Inventory inv = Bukkit.createInventory(null, 27, Component.text(title, NamedTextColor.DARK_GRAY));

        // Background / Border
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        // Center: Player Head
        inv.setItem(13, createPlayerHead(identity));

        // Left: Identity Details
        inv.setItem(11, createInfoItem(identity));

        // Right: Security & Status
        inv.setItem(15, createStatusItem(identity));

        viewer.openInventory(inv);
    }

    private ItemStack createPlayerHead(Identity identity) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(identity.getName(), NamedTextColor.AQUA)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("UUID: ", NamedTextColor.GRAY)
                    .append(Component.text(identity.getUuid().toString().substring(0, 18) + "...", NamedTextColor.DARK_GRAY)));
            
            meta.lore(lore);
            // We don't necessarily have the Player object here if they are offline, 
            // but for online players we could set the owning player.
            Player player = Bukkit.getPlayer(identity.getUuid());
            if (player != null) {
                meta.setOwningPlayer(player);
            }
            head.setItemMeta(meta);
        }
        return head;
    }

    private ItemStack createInfoItem(Identity identity) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Personal Information", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            
            String cid = identity.getCitizenId() != null ? identity.getCitizenId() : "Not Issued";
            lore.add(Component.text("Citizen ID: ", NamedTextColor.GRAY)
                    .append(Component.text(cid, NamedTextColor.GOLD)));
            
            String date = DATE_FORMATTER.format(Instant.ofEpochMilli(identity.getCreatedAt()));
            lore.add(Component.text("Registration: ", NamedTextColor.GRAY)
                    .append(Component.text(date, NamedTextColor.WHITE)));
            
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createStatusItem(Identity identity) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Account Status", NamedTextColor.LIGHT_PURPLE)
                    .decoration(TextDecoration.ITALIC, false));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            
            // Security Status
            boolean isVerified = identity.getPin() != null && !identity.getPin().isEmpty();
            lore.add(Component.text("Security: ", NamedTextColor.GRAY)
                    .append(isVerified 
                            ? Component.text("✔ SECURED", NamedTextColor.GREEN) 
                            : Component.text("✘ UNPROTECTED", NamedTextColor.RED)));
            
            // Account Type
            lore.add(Component.text("Tier: ", NamedTextColor.GRAY)
                    .append(identity.hasPremiumUuid() 
                            ? Component.text("★ PREMIUM", NamedTextColor.GOLD) 
                            : Component.text("○ STANDARD", NamedTextColor.WHITE)));
            
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name).decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }
}
