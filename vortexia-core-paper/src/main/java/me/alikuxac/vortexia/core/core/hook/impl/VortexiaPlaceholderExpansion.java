// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.core.hook.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.alikuxac.vortexia.api.model.Identity;
import me.alikuxac.vortexia.core.VortexiaCore;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class VortexiaPlaceholderExpansion extends PlaceholderExpansion {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(ZoneId.systemDefault());

    private final VortexiaCore plugin;
    private final Cache<String, String> metaCache;

    public VortexiaPlaceholderExpansion(VortexiaCore plugin) {
        this.plugin = plugin;
        this.metaCache = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(5, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "vortexia";
    }

    @Override
    public @NotNull String getAuthor() {
        return "alikuxac";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null) {
            return "";
        }

        UUID uuid = offlinePlayer.getUniqueId();

        if (params.equalsIgnoreCase("pin_status")) {
            if (offlinePlayer.isOnline() && offlinePlayer.getPlayer() != null) {
                return plugin.getSecurityManager().isAuthenticated(offlinePlayer.getPlayer()) ? "VERIFIED" : "LOCKED";
            }
            return "LOCKED";
        }

        Optional<Identity> optIdentity = plugin.getStorageManager().getCache().getByUuid(uuid);
        if (optIdentity.isEmpty()) {
            plugin.getStorageManager().getIdentity(uuid);
        }

        if (params.equalsIgnoreCase("cccd")) {
            return optIdentity.map(Identity::getCitizenId).orElse("");
        }

        if (params.equalsIgnoreCase("created_date")) {
            return optIdentity
                    .filter(identity -> identity.getCreatedAt() > 0)
                    .map(identity -> DATE_FORMATTER.format(Instant.ofEpochMilli(identity.getCreatedAt())))
                    .orElse("");
        }

        if (params.toLowerCase().startsWith("meta_")) {
            String metaKey = params.substring(5);
            if (metaKey.isEmpty()) {
                return "";
            }

            String cacheKey = uuid + ":" + metaKey;
            String cachedVal = metaCache.getIfPresent(cacheKey);
            if (cachedVal != null) {
                return cachedVal;
            }

            plugin.getStorageManager().getMetadata(uuid, metaKey).thenAccept(optVal -> {
                metaCache.put(cacheKey, optVal.orElse(""));
            });

            return "";
        }

        return null;
    }
}
