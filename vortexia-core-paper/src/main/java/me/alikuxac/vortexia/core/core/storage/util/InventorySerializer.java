// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.core.storage.util;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class InventorySerializer {

  public static String itemStackArrayToBase64(ItemStack[] items) throws IllegalStateException {
    try {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

      dataOutput.writeInt(items.length);

      for (ItemStack item : items) {
        dataOutput.writeObject(item);
      }

      dataOutput.close();
      return Base64Coder.encodeLines(outputStream.toByteArray());
    } catch (Exception e) {
      throw new IllegalStateException("Unable to save item stacks.", e);
    }
  }

  public static ItemStack[] itemStackArrayFromBase64(String data) throws java.io.IOException {
    try {
      ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
      BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
      int size = dataInput.readInt();
      ItemStack[] items = new ItemStack[size];

      for (int i = 0; i < size; i++) {
        try {
          items[i] = (ItemStack) dataInput.readObject();
        } catch (Exception e) {
          // Failsafe Filter: replace corrupted or missing plugin item with null (AIR) and log warning
          items[i] = null;
          Bukkit.getLogger().warning("[Vortexia] Failed to deserialize item stack at index " + i + " (possible missing plugin or corrupted NBT), replaced with AIR. Error: " + e.getMessage());
        }
      }

      dataInput.close();
      return items;
    } catch (Exception e) {
      throw new java.io.IOException("Unable to decode item stacks database snapshot.", e);
    }
  }
}
