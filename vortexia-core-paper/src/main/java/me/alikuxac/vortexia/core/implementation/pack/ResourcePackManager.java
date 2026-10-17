// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.implementation.pack;

import me.alikuxac.vortexia.core.VortexiaCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ResourcePackManager {

    private final VortexiaCore plugin;
    private final File packFolder;
    private final File zipFile;
    private String sha1Hash = "";
    private byte[] sha1HashBytes = new byte[0];

    public ResourcePackManager(VortexiaCore plugin) {
        this.plugin = plugin;
        this.packFolder = new File(plugin.getDataFolder(), "pack");
        this.zipFile = new File(packFolder, "vortexia-resourcepack.zip");
    }

    public void initialize() {
        if (!plugin.getConfig().getBoolean("resource-pack.enabled", true)) {
            plugin.getLoggerService().info("Resource pack management is disabled in config.");
            return;
        }

        if (plugin.getConfig().getBoolean("resource-pack.auto-generate-zip", true)) {
            exportAndBuildZip();
        }

        computeChecksum();
    }

    public synchronized boolean exportAndBuildZip() {
        if (!packFolder.exists() && !packFolder.mkdirs()) {
            plugin.getLoggerService().warn("Failed to create resource pack directory: " + packFolder.getAbsolutePath());
        }

        plugin.getLoggerService().info("Packaging Vortexia Resource Pack into: " + zipFile.getName() + "...");

        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos))) {

            Set<String> addedEntries = new HashSet<>();
            boolean packagedFromJar = tryPackageFromJar(zos, addedEntries);

            if (!packagedFromJar || addedEntries.isEmpty()) {
                // Fallback: package known resources directly via getResourceAsStream
                packageKnownResources(zos, addedEntries);
            }

            zos.finish();
            zos.flush();

            computeChecksum();
            plugin.getLoggerService().info("Resource pack generated successfully. Size: " + (zipFile.length() / 1024) + " KB | SHA-1: " + sha1Hash);
            return true;
        } catch (Exception e) {
            plugin.getLoggerService().warn("Error while packaging resource pack: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean tryPackageFromJar(ZipOutputStream zos, Set<String> addedEntries) {
        try {
            String path = plugin.getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
            String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
            File jarFile = new File(decodedPath);

            if (!jarFile.exists() || !jarFile.isFile()) {
                return false;
            }

            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> entries = jar.entries();
                byte[] buffer = new byte[8192];

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();

                    if (name.startsWith("resourcepack/") && !entry.isDirectory() && name.length() > "resourcepack/".length()) {
                        String relativePath = name.substring("resourcepack/".length());
                        if (addedEntries.add(relativePath)) {
                            zos.putNextEntry(new ZipEntry(relativePath));
                            try (InputStream is = jar.getInputStream(entry)) {
                                int len;
                                while ((len = is.read(buffer)) > 0) {
                                    zos.write(buffer, 0, len);
                                }
                            }
                            zos.closeEntry();
                        }
                    }
                }
            }
            return !addedEntries.isEmpty();
        } catch (Exception ignored) {
            return false;
        }
    }

    private void packageKnownResources(ZipOutputStream zos, Set<String> addedEntries) throws IOException {
        String[] resources = new String[]{
                "pack.mcmeta",
                "pack.png",
                "assets/minecraft/items/paper.json",
                "assets/minecraft/models/item/paper.json",
                "assets/vortexia/models/item/cccd.json",
                "assets/vortexia/textures/item/cccd.png"
        };

        byte[] buffer = new byte[8192];
        for (String res : resources) {
            if (addedEntries.contains(res)) {
                continue;
            }

            try (InputStream is = plugin.getResource("resourcepack/" + res)) {
                if (is != null) {
                    zos.putNextEntry(new ZipEntry(res));
                    int len;
                    while ((len = is.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                    zos.closeEntry();
                    addedEntries.add(res);
                }
            }
        }
    }

    public void computeChecksum() {
        if (!zipFile.exists() || zipFile.length() == 0) {
            this.sha1Hash = "";
            this.sha1HashBytes = new byte[0];
            return;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream fis = new FileInputStream(zipFile)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }

            this.sha1HashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : this.sha1HashBytes) {
                sb.append(String.format("%02x", b));
            }
            this.sha1Hash = sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            plugin.getLoggerService().warn("Failed to compute resource pack SHA-1: " + e.getMessage());
        }
    }

    public boolean sendResourcePack(Player player) {
        if (player == null || !player.isOnline()) {
            return false;
        }

        String url = plugin.getConfig().getString("resource-pack.url", "").trim();
        if (url.isEmpty()) {
            plugin.getLoggerService().warn("Cannot send resource pack to player " + player.getName() + ": 'resource-pack.url' is not configured.");
            return false;
        }

        String configHash = plugin.getConfig().getString("resource-pack.hash", "").trim();
        byte[] hashBytes = sha1HashBytes;
        if (!configHash.isEmpty()) {
            try {
                hashBytes = hexStringToByteArray(configHash);
            } catch (Exception e) {
                plugin.getLoggerService().warn("Invalid configured SHA-1 hash format: " + configHash);
            }
        }

        boolean force = plugin.getConfig().getBoolean("resource-pack.force", false);
        String promptText = plugin.getConfig().getString("resource-pack.prompt", "§bVortexia§7: Please download the resource pack.");
        Component prompt = LegacyComponentSerializer.legacyAmpersand().deserialize(promptText);
        UUID packId = UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8));
        byte[] finalHash = (hashBytes != null && hashBytes.length > 0) ? hashBytes : null;

        try {
            player.setResourcePack(packId, url, finalHash, prompt, force);
            return true;
        } catch (Throwable t) {
            try {
                net.kyori.adventure.resource.ResourcePackInfo.Builder infoBuilder = net.kyori.adventure.resource.ResourcePackInfo.resourcePackInfo()
                        .id(packId)
                        .uri(URI.create(url));

                if (!configHash.isEmpty()) {
                    infoBuilder.hash(configHash.toLowerCase());
                } else if (!sha1Hash.isEmpty()) {
                    infoBuilder.hash(sha1Hash.toLowerCase());
                }

                net.kyori.adventure.resource.ResourcePackRequest request = net.kyori.adventure.resource.ResourcePackRequest.resourcePackRequest()
                        .packs(infoBuilder.build())
                        .required(force)
                        .prompt(prompt)
                        .build();

                player.sendResourcePacks(request);
                return true;
            } catch (Throwable t2) {
                plugin.getLoggerService().warn("Failed to dispatch resource pack to " + player.getName() + ": " + t2.getMessage());
                return false;
            }
        }
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public File getZipFile() {
        return zipFile;
    }

    public String getSha1Hash() {
        return sha1Hash;
    }

    public byte[] getSha1HashBytes() {
        return sha1HashBytes;
    }
}
