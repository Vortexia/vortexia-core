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
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy")
      .withZone(ZoneId.systemDefault());

  public CitizenCardManager(VortexiaCore plugin) {
    this.plugin = plugin;
    this.citizenIdKey = new NamespacedKey(plugin, "citizen_id");
  }

  public ItemStack createCard(Identity identity) {
    ItemStack item = new ItemStack(Material.PAPER);
    ItemMeta meta = item.getItemMeta();

    if (meta != null) {
      meta.displayName(Component.text("Citizen Identity Card", NamedTextColor.AQUA)
          .decoration(TextDecoration.ITALIC, false));

      List<Component> lore = new ArrayList<>();
      lore.add(Component.empty());
      lore.add(Component.text("Holder: ", NamedTextColor.GRAY)
          .append(Component.text(identity.getName(), NamedTextColor.WHITE))
          .decoration(TextDecoration.ITALIC, false));

      String cid = identity.getCitizenId() != null ? identity.getCitizenId() : "UNK";
      lore.add(Component.text("ID: ", NamedTextColor.GRAY)
          .append(Component.text(cid, NamedTextColor.YELLOW))
          .decoration(TextDecoration.ITALIC, false));

      String date = DATE_FORMATTER.format(Instant.ofEpochMilli(identity.getCreatedAt()));
      lore.add(Component.text("Issued: ", NamedTextColor.GRAY)
          .append(Component.text(date, NamedTextColor.WHITE))
          .decoration(TextDecoration.ITALIC, false));

      lore.add(Component.empty());
      lore.add(Component.text("Soulbound", NamedTextColor.DARK_GRAY)
          .decoration(TextDecoration.ITALIC, true));

      meta.lore(lore);

      // Store ID in PDC
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

  public boolean isCitizenCard(ItemStack item) {
    if (item == null || !item.hasItemMeta())
      return false;
    return item.getItemMeta().getPersistentDataContainer().has(citizenIdKey, PersistentDataType.STRING);
  }
}
