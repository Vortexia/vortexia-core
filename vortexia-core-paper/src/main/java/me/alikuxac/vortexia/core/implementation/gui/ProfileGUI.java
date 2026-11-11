// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.implementation.gui;

import me.alikuxac.vortexia.api.model.Identity;
import me.alikuxac.vortexia.core.VortexiaCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
import org.bukkit.inventory.meta.SkullMeta;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ProfileGUI implements Listener {

    private final VortexiaCore plugin;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    public ProfileGUI(VortexiaCore plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public static class ProfileGUIHolder implements InventoryHolder {
        private final Identity identity;

        public ProfileGUIHolder(Identity identity) {
            this.identity = identity;
        }

        public Identity getIdentity() {
            return identity;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public static class SecurityLogHolder implements InventoryHolder {
        private final Identity identity;

        public SecurityLogHolder(Identity identity) {
            this.identity = identity;
        }

        public Identity getIdentity() {
            return identity;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public void open(Player viewer, Identity identity) {
        String title = "Citizen Profile: " + (identity.getName() != null ? identity.getName() : "Unknown");
        Inventory inv = Bukkit.createInventory(new ProfileGUIHolder(identity), 27, Component.text(title, NamedTextColor.DARK_GRAY));

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        inv.setItem(4, createPlayerHead(identity));
        inv.setItem(11, createIdentityInfoItem(identity));
        inv.setItem(13, createUuidInfoItem(identity));
        inv.setItem(15, createSecurityStatusItem(viewer, identity));
        inv.setItem(20, createChangePinButton(identity));
        inv.setItem(24, createSecurityLogButton(identity));

        viewer.openInventory(inv);
    }

    public void openSecurityLog(Player viewer, Identity identity) {
        String title = "Security Log: " + (identity.getName() != null ? identity.getName() : "Unknown");
        Inventory inv = Bukkit.createInventory(new SecurityLogHolder(identity), 27, Component.text(title, NamedTextColor.DARK_GRAY));

        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        inv.setItem(10, createRegistrationLogItem(identity));
        inv.setItem(12, createPinUpdateLogItem(identity));
        inv.setItem(14, createUuidAuditItem(identity));
        inv.setItem(16, createSessionAuditItem(viewer, identity));

        ItemStack back = createItem(Material.ARROW, "◀ Back to Profile", NamedTextColor.RED);
        ItemMeta bm = back.getItemMeta();
        if (bm != null) {
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Click to return to Citizen Profile.", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            bm.lore(lore);
            back.setItemMeta(bm);
        }
        inv.setItem(22, back);

        viewer.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;

        Inventory topInv = event.getView().getTopInventory();
        InventoryHolder holder = topInv.getHolder();

        if (holder instanceof ProfileGUIHolder profileHolder) {
            event.setCancelled(true);
            if (event.getClickedInventory() != topInv) return;

            int slot = event.getRawSlot();
            Identity identity = profileHolder.getIdentity();

            if (slot == 20) {
                boolean isOwner = player.getUniqueId().equals(identity.getUuid())
                        || (identity.hasPremiumUuid() && player.getUniqueId().equals(identity.getPremiumUuid()));
                boolean isAdmin = player.hasPermission("vortexia.admin") || player.hasPermission("vortexia.admin.security");

                if (!isOwner && !isAdmin) {
                    player.sendMessage(Component.text("You can only manage security PIN for your own Citizen ID!", NamedTextColor.RED));
                    return;
                }

                player.closeInventory();
                plugin.getSchedulerService().runEntity(player, () -> {
                    if (identity.getPin() != null && !identity.getPin().isEmpty()) {
                        plugin.getSecurityGUI().openChangeVerify(player);
                    } else {
                        plugin.getSecurityGUI().openSetup(player);
                    }
                });
            } else if (slot == 24) {
                plugin.getSchedulerService().runEntity(player, () -> openSecurityLog(player, identity));
            }
            return;
        }

        if (holder instanceof SecurityLogHolder logHolder) {
            event.setCancelled(true);
            if (event.getClickedInventory() != topInv) return;

            int slot = event.getRawSlot();
            if (slot == 22) {
                Identity identity = logHolder.getIdentity();
                plugin.getSchedulerService().runEntity(player, () -> open(player, identity));
            }
        }
    }

    private ItemStack createPlayerHead(Identity identity) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(identity.getName() != null ? identity.getName() : "Unknown Citizen", NamedTextColor.AQUA)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());

            Player onlinePlayer = Bukkit.getPlayer(identity.getUuid());
            if (onlinePlayer == null && identity.hasPremiumUuid()) {
                onlinePlayer = Bukkit.getPlayer(identity.getPremiumUuid());
            }

            boolean isOnline = onlinePlayer != null && onlinePlayer.isOnline();
            lore.add(Component.text("Status: ", NamedTextColor.GRAY)
                    .append(isOnline
                            ? Component.text("● ONLINE", NamedTextColor.GREEN)
                            : Component.text("○ OFFLINE", NamedTextColor.DARK_GRAY))
                    .decoration(TextDecoration.ITALIC, false));

            if (onlinePlayer != null) {
                meta.setOwningPlayer(onlinePlayer);
            }

            meta.lore(lore);
            head.setItemMeta(meta);
        }
        return head;
    }

    private ItemStack createIdentityInfoItem(Identity identity) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Citizen Card Info", NamedTextColor.GOLD)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());

            String cid = identity.getCitizenId() != null ? identity.getCitizenId() : "Not Issued";
            lore.add(Component.text("Citizen ID: ", NamedTextColor.GRAY)
                    .append(Component.text(cid, NamedTextColor.YELLOW))
                    .decoration(TextDecoration.ITALIC, false));

            long createdAt = identity.getCreatedAt() > 0 ? identity.getCreatedAt() : System.currentTimeMillis();
            String createdDate = DATE_FORMATTER.format(Instant.ofEpochMilli(createdAt));
            lore.add(Component.text("Issued: ", NamedTextColor.GRAY)
                    .append(Component.text(createdDate, NamedTextColor.WHITE))
                    .decoration(TextDecoration.ITALIC, false));

            long updatedAt = identity.getUpdatedAt() > 0 ? identity.getUpdatedAt() : createdAt;
            String updatedDate = DATE_FORMATTER.format(Instant.ofEpochMilli(updatedAt));
            lore.add(Component.text("Updated: ", NamedTextColor.GRAY)
                    .append(Component.text(updatedDate, NamedTextColor.DARK_GRAY))
                    .decoration(TextDecoration.ITALIC, false));

            lore.add(Component.empty());
            lore.add(Component.text("✔ Valid Citizen Card", NamedTextColor.DARK_AQUA)
                    .decoration(TextDecoration.ITALIC, true));

            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createUuidInfoItem(Identity identity) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Linked UUIDs", NamedTextColor.AQUA)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());

            lore.add(Component.text("Offline UUID: ", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text(identity.getUuid().toString(), NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false));

            lore.add(Component.empty());

            lore.add(Component.text("Online UUID: ", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            if (identity.hasPremiumUuid()) {
                lore.add(Component.text(identity.getPremiumUuid().toString(), NamedTextColor.GOLD)
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Mojang Link: ", NamedTextColor.GRAY)
                        .append(Component.text("✔ LINKED", NamedTextColor.GREEN))
                        .decoration(TextDecoration.ITALIC, false));
            } else {
                lore.add(Component.text("Not linked to Mojang", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false));
            }

            lore.add(Component.empty());
            lore.add(Component.text("Account Tier: ", NamedTextColor.GRAY)
                    .append(identity.hasPremiumUuid()
                            ? Component.text("★ PREMIUM", NamedTextColor.GOLD)
                            : Component.text("○ STANDARD", NamedTextColor.WHITE))
                    .decoration(TextDecoration.ITALIC, false));

            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSecurityStatusItem(Player viewer, Identity identity) {
        ItemStack item = new ItemStack(Material.SHIELD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Security Status", NamedTextColor.LIGHT_PURPLE)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());

            boolean hasPin = identity.getPin() != null && !identity.getPin().isEmpty();
            lore.add(Component.text("PIN Level 2: ", NamedTextColor.GRAY)
                    .append(hasPin
                            ? Component.text("✔ ACTIVE", NamedTextColor.GREEN)
                            : Component.text("✘ NOT SET", NamedTextColor.RED))
                    .decoration(TextDecoration.ITALIC, false));

            boolean isAuth = plugin.getSecurityManager().isAuthenticated(viewer);
            lore.add(Component.text("Current Session: ", NamedTextColor.GRAY)
                    .append(isAuth
                            ? Component.text("✔ AUTHENTICATED", NamedTextColor.GREEN)
                            : Component.text("✘ UNAUTHENTICATED", NamedTextColor.RED))
                    .decoration(TextDecoration.ITALIC, false));

            long lockLeft = plugin.getSecurityManager().pinLockoutSecondsLeft(viewer);
            if (lockLeft > 0) {
                lore.add(Component.text("Lockout: ", NamedTextColor.RED)
                        .append(Component.text(lockLeft + "s", NamedTextColor.YELLOW))
                        .decoration(TextDecoration.ITALIC, false));
            } else {
                lore.add(Component.text("Lockout Status: ", NamedTextColor.GRAY)
                        .append(Component.text("Normal", NamedTextColor.GREEN))
                        .decoration(TextDecoration.ITALIC, false));
            }

            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createChangePinButton(Identity identity) {
        boolean hasPin = identity.getPin() != null && !identity.getPin().isEmpty();
        ItemStack item = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(hasPin ? "Quick Change PIN" : "Set Up PIN", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text(hasPin
                    ? "Open SecurityGUI to change PIN."
                    : "Set up a 6-digit PIN to secure your Citizen Card.", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("▶ Click to proceed", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));

            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSecurityLogButton(Identity identity) {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Security Log", NamedTextColor.GOLD)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("View issuance, update history and", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("security activity for this Citizen ID.", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text("▶ Click to open log", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));

            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createRegistrationLogItem(Identity identity) {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Citizen ID Issuance History", NamedTextColor.AQUA)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            long createdAt = identity.getCreatedAt() > 0 ? identity.getCreatedAt() : System.currentTimeMillis();
            lore.add(Component.text("Issued At: ", NamedTextColor.GRAY)
                    .append(Component.text(DATE_FORMATTER.format(Instant.ofEpochMilli(createdAt)), NamedTextColor.WHITE))
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Citizen ID: ", NamedTextColor.GRAY)
                    .append(Component.text(identity.getCitizenId() != null ? identity.getCitizenId() : "N/A", NamedTextColor.GOLD))
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Holder: ", NamedTextColor.GRAY)
                    .append(Component.text(identity.getName(), NamedTextColor.YELLOW))
                    .decoration(TextDecoration.ITALIC, false));

            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPinUpdateLogItem(Identity identity) {
        ItemStack item = new ItemStack(Material.REDSTONE_TORCH);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("PIN Level 2 Log", NamedTextColor.LIGHT_PURPLE)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            boolean hasPin = identity.getPin() != null && !identity.getPin().isEmpty();
            lore.add(Component.text("PIN Status: ", NamedTextColor.GRAY)
                    .append(hasPin ? Component.text("Protection Active", NamedTextColor.GREEN) : Component.text("Not Set", NamedTextColor.RED))
                    .decoration(TextDecoration.ITALIC, false));

            long updatedAt = identity.getUpdatedAt() > 0 ? identity.getUpdatedAt() : identity.getCreatedAt();
            lore.add(Component.text("Last Updated: ", NamedTextColor.GRAY)
                    .append(Component.text(DATE_FORMATTER.format(Instant.ofEpochMilli(updatedAt)), NamedTextColor.WHITE))
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Security Encryption: ", NamedTextColor.GRAY)
                    .append(Component.text("SHA-256 + Random Salt", NamedTextColor.DARK_AQUA))
                    .decoration(TextDecoration.ITALIC, false));

            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createUuidAuditItem(Identity identity) {
        ItemStack item = new ItemStack(Material.ENDER_EYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("UUID Link Log", NamedTextColor.GOLD)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("Primary Offline UUID: ", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text(identity.getUuid().toString(), NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false));

            lore.add(Component.empty());
            lore.add(Component.text("Mojang Premium UUID: ", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            if (identity.hasPremiumUuid()) {
                lore.add(Component.text(identity.getPremiumUuid().toString(), NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Status: Synced with Mojang", NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false));
            } else {
                lore.add(Component.text("No Mojang Link", NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false));
            }

            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSessionAuditItem(Player viewer, Identity identity) {
        ItemStack item = new ItemStack(Material.IRON_CHESTPLATE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Session Status & Warnings", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.BOLD, true)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            boolean isAuth = plugin.getSecurityManager().isAuthenticated(viewer);
            lore.add(Component.text("Current Session: ", NamedTextColor.GRAY)
                    .append(isAuth ? Component.text("Authenticated", NamedTextColor.GREEN) : Component.text("Unauthenticated", NamedTextColor.RED))
                    .decoration(TextDecoration.ITALIC, false));

            long lockLeft = plugin.getSecurityManager().pinLockoutSecondsLeft(viewer);
            lore.add(Component.text("Lockout Warning: ", NamedTextColor.GRAY)
                    .append(lockLeft > 0 ? Component.text("Locked (" + lockLeft + "s)", NamedTextColor.RED) : Component.text("No infractions", NamedTextColor.GREEN))
                    .decoration(TextDecoration.ITALIC, false));

            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItem(Material material, String name) {
        return createItem(material, name, NamedTextColor.WHITE);
    }

    private ItemStack createItem(Material material, String name, NamedTextColor color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }
}
