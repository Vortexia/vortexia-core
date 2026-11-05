// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.implementation.item;

import me.alikuxac.vortexia.core.VortexiaCore;
import me.alikuxac.vortexia.api.model.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataType;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CitizenCardManager {

  public static final int DEFAULT_CUSTOM_MODEL_DATA = 10001;

  private final VortexiaCore plugin;
  private final NamespacedKey citizenIdKey;
  private final NamespacedKey itemTypeKey;
  private final Map<String, Integer> citizenMapIdCache = new ConcurrentHashMap<>();
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy")
      .withZone(ZoneId.systemDefault());

  public CitizenCardManager(VortexiaCore plugin) {
    this.plugin = plugin;
    this.citizenIdKey = new NamespacedKey(plugin, "citizen_id");
    this.itemTypeKey = new NamespacedKey(plugin, "item_type");
  }

  public ItemStack createCard(Identity identity) {
    Player player = identity.getUuid() != null ? Bukkit.getPlayer(identity.getUuid()) : null;
    return createCard(identity, player);
  }

  public ItemStack createCard(Identity identity, Player player) {
    boolean hasPack = player != null
        && plugin.getResourcePackManager() != null
        && plugin.getResourcePackManager().hasResourcePack(player.getUniqueId());

    if (hasPack) {
      int configCmd = plugin.getConfig().getInt("citizen-card.custom-model-data", DEFAULT_CUSTOM_MODEL_DATA);
      int cmd = configCmd > 0 ? configCmd : DEFAULT_CUSTOM_MODEL_DATA;
      return createCard(identity, cmd);
    }

    return createMapCard(identity, player);
  }

  public ItemStack createMapCard(Identity identity, Player player) {
    ItemStack item = new ItemStack(Material.FILLED_MAP);
    MapMeta meta = (MapMeta) item.getItemMeta();
    if (meta != null) {
      World world = player != null ? player.getWorld() : (Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0));
      MapView mapView = null;
      String cid = identity.getCitizenId();
      if (cid != null && citizenMapIdCache.containsKey(cid)) {
        mapView = Bukkit.getMap(citizenMapIdCache.get(cid));
      }
      if (mapView == null && world != null) {
        mapView = Bukkit.createMap(world);
        if (cid != null) {
          citizenMapIdCache.put(cid, mapView.getId());
        }
      }
      if (mapView != null) {
        mapView.getRenderers().forEach(mapView::removeRenderer);
        mapView.addRenderer(new CitizenCardMapRenderer(identity));
        mapView.setTrackingPosition(false);
        mapView.setUnlimitedTracking(false);
        mapView.setLocked(true);
        meta.setMapView(mapView);
      }

      populateCardMeta(meta, identity, 0);
      item.setItemMeta(meta);
    }
    return item;
  }

  public ItemStack createCard(Identity identity, int customModelData) {
    String materialName = plugin.getConfig().getString("citizen-card.material", "PAPER");
    Material material = Material.matchMaterial(materialName);
    if (material == null) material = Material.PAPER;

    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();

    if (meta != null) {
      populateCardMeta(meta, identity, customModelData);
      item.setItemMeta(meta);
    }

    return item;
  }

  private void populateCardMeta(ItemMeta meta, Identity identity, int customModelData) {
    if (customModelData > 0) {
      meta.setCustomModelData(customModelData);
    }

    meta.displayName(Component.text()
        .append(Component.text("Citizen Identity Card", NamedTextColor.AQUA))
        .decoration(TextDecoration.ITALIC, false)
        .decoration(TextDecoration.BOLD, true)
        .build());

    List<Component> lore = new ArrayList<>();
    lore.add(Component.text("━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY)
        .decoration(TextDecoration.STRIKETHROUGH, false));
    
    lore.add(Component.text(" Holder: ", NamedTextColor.GRAY)
        .append(Component.text(identity.getName() != null ? identity.getName() : "Unknown", NamedTextColor.WHITE))
        .decoration(TextDecoration.ITALIC, false));

    String cid = identity.getCitizenId() != null ? identity.getCitizenId() : "N/A";
    lore.add(Component.text(" CID: ", NamedTextColor.GRAY)
        .append(Component.text(cid, NamedTextColor.YELLOW))
        .decoration(TextDecoration.ITALIC, false));

    lore.add(Component.empty());

    boolean isVerified = identity.getPin() != null && !identity.getPin().isEmpty();
    lore.add(Component.text(" Security: ", NamedTextColor.GRAY)
        .append(isVerified 
            ? Component.text("✔ Verified", NamedTextColor.GREEN) 
            : Component.text("✘ Unsecured", NamedTextColor.RED))
        .decoration(TextDecoration.ITALIC, false));

    lore.add(Component.text(" Type: ", NamedTextColor.GRAY)
        .append(identity.hasPremiumUuid() 
            ? Component.text("★ Premium", NamedTextColor.GOLD) 
            : Component.text("○ Standard", NamedTextColor.WHITE))
        .decoration(TextDecoration.ITALIC, false));

    lore.add(Component.empty());

    long createdAt = identity.getCreatedAt() > 0 ? identity.getCreatedAt() : System.currentTimeMillis();
    String date = DATE_FORMATTER.format(Instant.ofEpochMilli(createdAt));
    lore.add(Component.text(" Issued: ", NamedTextColor.DARK_GRAY)
        .append(Component.text(date, NamedTextColor.GRAY))
        .decoration(TextDecoration.ITALIC, false));

    lore.add(Component.text("━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY)
        .decoration(TextDecoration.STRIKETHROUGH, false));
    
    lore.add(Component.text(" Authenticity Guaranteed ", NamedTextColor.DARK_AQUA)
        .decoration(TextDecoration.ITALIC, true));

    meta.lore(lore);

    meta.getPersistentDataContainer().set(itemTypeKey, PersistentDataType.STRING, "citizen_card");
    if (identity.getCitizenId() != null) {
      meta.getPersistentDataContainer().set(citizenIdKey, PersistentDataType.STRING, identity.getCitizenId());
    }
  }

  public boolean validateCard(ItemStack item, Identity identity) {
    if (item == null || identity == null || identity.getCitizenId() == null)
      return false;

    String storedId = getCitizenId(item);
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

    return item.getItemMeta().getPersistentDataContainer().has(citizenIdKey, PersistentDataType.STRING);
  }

  public NamespacedKey getCitizenIdKey() {
    return citizenIdKey;
  }

  public NamespacedKey getItemTypeKey() {
    return itemTypeKey;
  }
}
