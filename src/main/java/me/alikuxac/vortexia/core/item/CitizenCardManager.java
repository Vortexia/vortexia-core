// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.item;

import me.alikuxac.vortexia.core.VortexiaCore;
import me.alikuxac.vortexia.api.model.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CitizenCardManager {

  private final VortexiaCore plugin;
  private final NamespacedKey citizenIdKey;
  private final NamespacedKey itemTypeKey;
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy")
      .withZone(ZoneId.systemDefault());

  public CitizenCardManager(VortexiaCore plugin) {
    this.plugin = plugin;
    this.citizenIdKey = new NamespacedKey(plugin, "citizen_id");
    this.itemTypeKey = new NamespacedKey(plugin, "item_type");
  }

  public ItemStack createCard(Identity identity) {
    String materialName = plugin.getConfig().getString("citizen-card.material", "PAPER");
    Material material = Material.matchMaterial(materialName);
    if (material == null) material = Material.PAPER;

    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();

    if (meta != null) {
      // Custom Model Data
      int cmd = plugin.getConfig().getInt("citizen-card.custom-model-data", 0);
      if (cmd > 0) {
        meta.setCustomModelData(cmd);
      }

      // Display Name with Gradient: Aqua -> Blue
      meta.displayName(Component.text()
          .append(Component.text("Citizen Identity Card", NamedTextColor.AQUA))
          .decoration(TextDecoration.ITALIC, false)
          .decoration(TextDecoration.BOLD, true)
          .build());

      List<Component> lore = new ArrayList<>();
      lore.add(Component.text("━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY)
          .decoration(TextDecoration.STRIKETHROUGH, false));
      
      lore.add(Component.text(" Holder: ", NamedTextColor.GRAY)
          .append(Component.text(identity.getName(), NamedTextColor.WHITE))
          .decoration(TextDecoration.ITALIC, false));

      String cid = identity.getCitizenId() != null ? identity.getCitizenId() : "N/A";
      lore.add(Component.text(" CID: ", NamedTextColor.GRAY)
          .append(Component.text(cid, NamedTextColor.YELLOW))
          .decoration(TextDecoration.ITALIC, false));

      lore.add(Component.empty());

      // Security Status
      boolean isVerified = identity.getPin() != null && !identity.getPin().isEmpty();
      lore.add(Component.text(" Security: ", NamedTextColor.GRAY)
          .append(isVerified 
              ? Component.text("✔ Verified", NamedTextColor.GREEN) 
              : Component.text("✘ Unsecured", NamedTextColor.RED))
          .decoration(TextDecoration.ITALIC, false));

      // Account Type
      lore.add(Component.text(" Type: ", NamedTextColor.GRAY)
          .append(identity.hasPremiumUuid() 
              ? Component.text("★ Premium", NamedTextColor.GOLD) 
              : Component.text("○ Standard", NamedTextColor.WHITE))
          .decoration(TextDecoration.ITALIC, false));

      lore.add(Component.empty());

      String date = DATE_FORMATTER.format(Instant.ofEpochMilli(identity.getCreatedAt()));
      lore.add(Component.text(" Issued: ", NamedTextColor.DARK_GRAY)
          .append(Component.text(date, NamedTextColor.GRAY))
          .decoration(TextDecoration.ITALIC, false));

      lore.add(Component.text("━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY)
          .decoration(TextDecoration.STRIKETHROUGH, false));
      
      lore.add(Component.text(" Authenticity Guaranteed ", NamedTextColor.DARK_AQUA)
          .decoration(TextDecoration.ITALIC, true));

      meta.lore(lore);

      // Store ID and Type in PDC
      meta.getPersistentDataContainer().set(itemTypeKey, PersistentDataType.STRING, "citizen_card");
      if (identity.getCitizenId() != null) {
        meta.getPersistentDataContainer().set(citizenIdKey, PersistentDataType.STRING, identity.getCitizenId());
      }

      item.setItemMeta(meta);
    }

    return item;
  }

  public boolean validateCard(ItemStack item, Identity identity) {
    if (item == null || !item.hasItemMeta())
      return false;

    String storedId = item.getItemMeta().getPersistentDataContainer().get(citizenIdKey, PersistentDataType.STRING);
    if (storedId == null)
      return false;

    return storedId.equals(identity.getCitizenId());
  }

  public String getCitizenId(ItemStack item) {
    if (!isCitizenCard(item)) return null;
    return item.getItemMeta().getPersistentDataContainer().get(citizenIdKey, PersistentDataType.STRING);
  }

  public boolean isCitizenCard(ItemStack item) {
    if (item == null || !item.hasItemMeta())
      return false;
    
    String type = item.getItemMeta().getPersistentDataContainer().get(itemTypeKey, PersistentDataType.STRING);
    if ("citizen_card".equals(type)) return true;

    // Fallback for legacy cards
    return item.getItemMeta().getPersistentDataContainer().has(citizenIdKey, PersistentDataType.STRING);
  }
}
