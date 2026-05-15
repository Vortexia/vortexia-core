// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.gui;

import me.alikuxac.vortexia.core.VortexiaCore;
import me.alikuxac.vortexia.core.util.PINUtil;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SecurityGUI implements Listener {

    public enum Mode {
        SETUP,
        VERIFY,
        CHANGE_OLD,
        CHANGE_NEW
    }

    private final VortexiaCore plugin;
    private final Map<UUID, StringBuilder> currentInput = new HashMap<>();
    private final Map<UUID, Mode> playerMode = new HashMap<>();
    private final Map<UUID, Inventory> openInventories = new HashMap<>();

    public SecurityGUI(VortexiaCore plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private static class SecurityGUIHolder implements InventoryHolder {
        private final Mode mode;
        public SecurityGUIHolder(Mode mode) { this.mode = mode; }
        public Mode getMode() { return mode; }
        @Override public Inventory getInventory() { return null; }
    }

    public void openSetup(Player player) {
        open(player, Mode.SETUP);
    }

    public void openVerify(Player player) {
        open(player, Mode.VERIFY);
    }

    public void openChangeVerify(Player player) {
        open(player, Mode.CHANGE_OLD);
    }

    private void open(Player player, Mode mode) {
        currentInput.put(player.getUniqueId(), new StringBuilder());
        playerMode.put(player.getUniqueId(), mode);
        
        Inventory inv = createInventory(player, mode);
        openInventories.put(player.getUniqueId(), inv);
        player.openInventory(inv);
    }

    private Inventory createInventory(Player player, Mode mode) {
        String title = switch (mode) {
            case SETUP -> "Setup Security PIN";
            case VERIFY -> "Verify Security PIN";
            case CHANGE_OLD -> "Change PIN: Verify Old";
            case CHANGE_NEW -> "Change PIN: Set New";
        };

        Inventory inv = Bukkit.createInventory(new SecurityGUIHolder(mode), 54, Component.text(title, NamedTextColor.DARK_GRAY));
        refreshItems(player, inv, mode);
        return inv;
    }

    private void refreshItems(Player player, Inventory inv, Mode mode) {
        // Background
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Display current input
        String input = currentInput.getOrDefault(player.getUniqueId(), new StringBuilder()).toString();
        String masked = "*".repeat(input.length());
        inv.setItem(4, createItem(Material.LIME_STAINED_GLASS_PANE, "PIN: " + (input.isEmpty() ? "____" : masked)));

        // Number Pad (Standard Layout)
        int[] slots = {10, 11, 12, 19, 20, 21, 28, 29, 30, 40};
        for (int i = 1; i <= 9; i++) {
            inv.setItem(slots[i - 1], createItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, String.valueOf(i)));
        }
        inv.setItem(slots[9], createItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "0"));

        // Actions
        inv.setItem(43, createItem(Material.RED_CONCRETE, "Clear", NamedTextColor.RED));
        inv.setItem(44, createItem(Material.LIME_CONCRETE, "Confirm", NamedTextColor.GREEN));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;
        
        Inventory topInv = event.getView().getTopInventory();
        if (topInv.getHolder() instanceof SecurityGUIHolder holder) {
            event.setCancelled(true);
            
            // Only handle clicks in the top inventory
            if (event.getClickedInventory() != topInv) return;

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            int slot = event.getRawSlot();
            UUID uuid = player.getUniqueId();
            StringBuilder sb = currentInput.computeIfAbsent(uuid, k -> new StringBuilder());

            // Slot-based detection
            int[] numberSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30, 40};
            String[] numbers = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};

            for (int i = 0; i < numberSlots.length; i++) {
                if (slot == numberSlots[i]) {
                    if (sb.length() < 6) {
                        sb.append(numbers[i]);
                        refreshItems(player, topInv, holder.getMode());
                    }
                    return;
                }
            }

            if (slot == 43) { // Clear
                sb.setLength(0);
                refreshItems(player, topInv, holder.getMode());
            } else if (slot == 44) { // Confirm
                handleConfirm(player, sb.toString(), holder.getMode());
            }
        }
    }

    private void handleConfirm(Player player, String pin, Mode mode) {
        if (pin.length() < 4) {
            player.sendMessage(Component.text("PIN must be at least 4 digits!", NamedTextColor.RED));
            return;
        }

        UUID uuid = player.getUniqueId();

        switch (mode) {
            case SETUP -> {
                String hashed = PINUtil.hash(pin);
                plugin.getIdentityMigrationHelper().createOrUpdateIdentity(player, hashed).thenRun(() -> {
                    plugin.getSecurityManager().authenticate(player);
                    player.sendMessage(Component.text("PIN set successfully! You are now authenticated.", NamedTextColor.GREEN));
                    player.closeInventory();
                    currentInput.remove(uuid);
                    playerMode.remove(uuid);
                });
            }
            case VERIFY -> {
                plugin.getStorageManager().getIdentity(uuid).thenAccept(optIdentity -> {
                    if (optIdentity.isPresent()) {
                        String storedHash = optIdentity.get().getPin();
                        if (PINUtil.verify(pin, storedHash)) {
                            plugin.getSecurityManager().authenticate(player);
                            player.sendMessage(Component.text("Authentication successful!", NamedTextColor.GREEN));
                            player.closeInventory();
                            currentInput.remove(uuid);
                            playerMode.remove(uuid);
                        } else {
                            player.sendMessage(Component.text("Incorrect PIN! Access denied.", NamedTextColor.RED));
                            currentInput.get(uuid).setLength(0);
                            plugin.getSchedulerService().runEntity(player, () -> refreshItems(player, player.getOpenInventory().getTopInventory(), mode));
                        }
                    }
                });
            }
            case CHANGE_OLD -> {
                plugin.getStorageManager().getIdentity(uuid).thenAccept(optIdentity -> {
                    if (optIdentity.isPresent()) {
                        String storedHash = optIdentity.get().getPin();
                        if (PINUtil.verify(pin, storedHash)) {
                            // Correct! Now open the Setup for new PIN
                            plugin.getSchedulerService().runEntity(player, () -> open(player, Mode.CHANGE_NEW));
                        } else {
                            player.sendMessage(Component.text("Incorrect current PIN!", NamedTextColor.RED));
                            currentInput.get(uuid).setLength(0);
                            plugin.getSchedulerService().runEntity(player, () -> refreshItems(player, player.getOpenInventory().getTopInventory(), mode));
                        }
                    }
                });
            }
            case CHANGE_NEW -> {
                String hashed = PINUtil.hash(pin);
                plugin.getIdentityMigrationHelper().createOrUpdateIdentity(player, hashed).thenRun(() -> {
                    player.sendMessage(Component.text("PIN changed successfully!", NamedTextColor.GREEN));
                    player.closeInventory();
                    currentInput.remove(uuid);
                    playerMode.remove(uuid);
                });
            }
        }
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
