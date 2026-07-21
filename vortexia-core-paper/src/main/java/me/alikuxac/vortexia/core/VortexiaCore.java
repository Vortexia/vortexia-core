// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core;

import dev.jorel.commandapi.CommandAPI;
import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.java.JavaPlugin;

import me.alikuxac.vortexia.api.VortexiaProvider;
import me.alikuxac.vortexia.core.api.CoreVortexiaAPI;
import me.alikuxac.vortexia.core.implementation.command.CommandManager;
import me.alikuxac.vortexia.core.core.config.ConfigManager;
import me.alikuxac.vortexia.core.core.permission.PermissionManager;
import me.alikuxac.vortexia.core.core.service.LoggerService;
import me.alikuxac.vortexia.core.core.service.SchedulerService;
import me.alikuxac.vortexia.core.core.service.SecurityManager;
import me.alikuxac.vortexia.core.core.service.ProxySyncService;
import me.alikuxac.vortexia.core.core.storage.StorageException;
import me.alikuxac.vortexia.core.core.storage.StorageManager;
import me.alikuxac.vortexia.core.core.storage.util.IdentityUtil;
import me.alikuxac.vortexia.core.core.storage.util.IdentityMigrationHelper;
import me.alikuxac.vortexia.core.core.hook.AuthHookManager;
import me.alikuxac.vortexia.core.core.hook.impl.AuthMeHook;
import me.alikuxac.vortexia.core.core.addon.CoreAddonManager;
import me.alikuxac.vortexia.core.core.brain.VortexiaTaskEngine;
import me.alikuxac.vortexia.core.core.network.wireless.CoreWirelessNetworkRegistry;
import me.alikuxac.vortexia.core.core.grid.CoreGridManager;
import me.alikuxac.vortexia.core.implementation.item.CitizenCardManager;
import me.alikuxac.vortexia.core.implementation.item.CoreItemRegistry;
import me.alikuxac.vortexia.core.implementation.gui.SecurityGUI;
import me.alikuxac.vortexia.core.implementation.gui.GuideGUI;
import me.alikuxac.vortexia.core.implementation.waila.CoreWailaManager;
import me.alikuxac.vortexia.core.implementation.waila.WailaTask;
import me.alikuxac.vortexia.core.implementation.recipe.CoreCustomRecipeManager;
import me.alikuxac.vortexia.core.implementation.listener.player.PlayerListener;
import me.alikuxac.vortexia.core.implementation.listener.security.SecurityPacketListener;
import me.alikuxac.vortexia.core.implementation.listener.security.AuthRestrictListener;
import me.alikuxac.vortexia.core.implementation.listener.citizen.CitizenCardListener;
import me.alikuxac.vortexia.core.implementation.listener.item.ItemCraftListener;
import me.alikuxac.vortexia.core.implementation.listener.system.UpdateNotifyListener;
import me.alikuxac.vortexia.core.utils.UpdateChecker;

import me.alikuxac.vortexia.core.core.service.InventorySyncManager;

public final class VortexiaCore extends JavaPlugin {

    private static VortexiaCore instance;
    private LoggerService loggerService;
    private ConfigManager configManager;
    private CommandManager commandManager;
    private StorageManager storageManager;
    private IdentityUtil identityUtil;
    private IdentityMigrationHelper identityMigrationHelper;
    private CitizenCardManager citizenCardManager;
    private SecurityGUI securityGUI;
    private SecurityManager securityManager;
    private InventorySyncManager inventorySyncManager;
    private AuthHookManager authHookManager;
    private PermissionManager permissionManager;
    private SchedulerService schedulerService;
    private ProxySyncService proxySyncService;
    private CoreAddonManager addonManager;
    private VortexiaTaskEngine taskEngine;
    private CoreItemRegistry itemRegistry;
    private GuideGUI guideGUI;
    private CoreWirelessNetworkRegistry wirelessNetworkRegistry;
    private CoreWailaManager wailaManager;
    private WailaTask wailaTask;
    private CoreGridManager gridManager;
    private CoreCustomRecipeManager customRecipeManager;
    private UpdateChecker updateChecker;

    @Override
    public void onLoad() {
        instance = this;
        // CommandAPI
        try {
            boolean isPaper = false;
            try {
                Class.forName("io.papermc.paper.threadedregionsupport.RegionizedServer");
                isPaper = true;
            } catch (ClassNotFoundException ignored) {
                try {
                    Class.forName("com.destroystokyo.paper.PaperConfig");
                    isPaper = true;
                } catch (ClassNotFoundException ignored2) {}
            }

            if (isPaper) {
                CommandAPI.onLoad(new dev.jorel.commandapi.CommandAPIPaperConfig(this).silentLogs(true).verboseOutput(true));
            } else {
                try {
                    CommandAPI.onLoad(new dev.jorel.commandapi.CommandAPIPaperConfig(this).silentLogs(true).verboseOutput(true));
                } catch (Throwable t) {
                    getLogger().warning("Failed to initialize CommandAPI on non-Paper server: " + t.getMessage());
                }
            }
        } catch (Exception e) {
            getLogger().severe("VortexiaCore - Failed to load CommandAPI: " + e.getMessage());
            e.printStackTrace();
        }
        
        // PacketEvents
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings().checkForUpdates(false);
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.configManager.loadConfig();

        this.loggerService = new LoggerService(this);
        this.schedulerService = new SchedulerService(this);
        this.identityUtil = new IdentityUtil(this);

        this.storageManager = new StorageManager(this);
        try {
            this.storageManager.initialize();
        } catch (StorageException e) {
            getLogger().severe("Failed to initialize storage: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.identityMigrationHelper = new IdentityMigrationHelper(this);
        this.citizenCardManager = new CitizenCardManager(this);
        this.securityGUI = new SecurityGUI(this);
        this.securityManager = new SecurityManager(this);
        this.inventorySyncManager = new me.alikuxac.vortexia.core.core.service.InventorySyncManager(this);
        this.proxySyncService = new ProxySyncService(this);
        this.authHookManager = new AuthHookManager(this);
        this.permissionManager = new PermissionManager(this);
        this.permissionManager.registerPermissions();

        // Initialize PacketEvents
        PacketEvents.getAPI().init();
        PacketEvents.getAPI().getEventManager().registerListener(new SecurityPacketListener(this));
        
        this.itemRegistry = new CoreItemRegistry(this);
        this.wirelessNetworkRegistry = new CoreWirelessNetworkRegistry();
        this.addonManager = new CoreAddonManager(this);
        this.taskEngine = new VortexiaTaskEngine(this);
        this.gridManager = new CoreGridManager(this);
        this.gridManager.startTicking();
        this.customRecipeManager = new CoreCustomRecipeManager();
        this.guideGUI = new GuideGUI(this);
        this.wailaManager = new CoreWailaManager();

        this.wailaTask = new WailaTask(this);

        // Start WAILA periodic task
        if (this.schedulerService.isFolia()) {
            org.bukkit.Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, t -> this.wailaTask.run(), 1L, 4L);
        } else {
            org.bukkit.Bukkit.getScheduler().runTaskTimer(this, this.wailaTask, 1L, 4L);
        }

        // Register Auth Hooks
        if (getServer().getPluginManager().getPlugin("AuthMe") != null) {
            AuthMeHook authMeHook = new AuthMeHook(this);
            authMeHook.register();
            this.authHookManager.registerHook(authMeHook);
        }

        loggerService.info("Server online mode: " + (identityUtil.isOnlineMode() ? "ENABLED" : "DISABLED"));

        VortexiaProvider.register(new CoreVortexiaAPI(this));

        getServer().getPluginManager().registerEvents(
                new PlayerListener(this),
                this);
        getServer().getPluginManager().registerEvents(
                new CitizenCardListener(this),
                this);
        getServer().getPluginManager().registerEvents(
                new AuthRestrictListener(this),
                this);
        getServer().getPluginManager().registerEvents(
                new ItemCraftListener(this),
                this);

        CommandAPI.onEnable();
        this.commandManager = new CommandManager(this);
        this.commandManager.registerCommands();

        // Start Periodic Auto-Save task for online players (default: 5 minutes / 6000 ticks)
        long autoSaveInterval = getConfig().getLong("inventory-sync.auto-save-interval-ticks", 6000L);
        if (this.schedulerService.isFolia()) {
            getServer().getGlobalRegionScheduler().runAtFixedRate(this, task -> {
                for (org.bukkit.entity.Player onlinePlayer : getServer().getOnlinePlayers()) {
                    if (this.securityManager.isAuthenticated(onlinePlayer)) {
                        this.inventorySyncManager.saveInventory(onlinePlayer);
                    }
                }
            }, 1200L, autoSaveInterval);
        } else {
            getServer().getScheduler().runTaskTimer(this, () -> {
                for (org.bukkit.entity.Player onlinePlayer : getServer().getOnlinePlayers()) {
                    if (this.securityManager.isAuthenticated(onlinePlayer)) {
                        this.inventorySyncManager.saveInventory(onlinePlayer);
                    }
                }
            }, 1200L, autoSaveInterval);
        }

        if (getConfig().getBoolean("update-checker.enabled", true)) {
            @SuppressWarnings("deprecation")
            String currentVersion = getDescription().getVersion();
            if (currentVersion.toUpperCase().contains("LOCAL")) {
                getLogger().info("Local build detected. Update checker disabled.");
            } else {
                String serverName = getServer().getName().toLowerCase();
                String loader = "bukkit";
                if (serverName.contains("paper")) {
                    loader = "paper";
                } else if (serverName.contains("purpur")) {
                    loader = "purpur";
                } else if (serverName.contains("folia")) {
                    loader = "folia";
                } else if (serverName.contains("spigot")) {
                    loader = "spigot";
                }

                this.updateChecker = new UpdateChecker(
                    currentVersion,
                    loader,
                    msg -> getLoggerService().info(msg),
                    msg -> getLoggerService().warn(msg),
                    msg -> getLoggerService().debug(msg),
                    runnable -> getSchedulerService().runAsync(runnable)
                );
                this.updateChecker.checkAsync().thenRun(() -> {
                    if (getConfig().getBoolean("update-checker.notify-admins", true)) {
                        getServer().getPluginManager().registerEvents(new UpdateNotifyListener(this), this);
                    }
                });
            }
        }
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();

        // Save all online players' inventories synchronously on shutdown/restart
        if (inventorySyncManager != null && securityManager != null) {
            java.util.List<java.util.concurrent.CompletableFuture<Void>> futures = new java.util.ArrayList<>();
            for (org.bukkit.entity.Player onlinePlayer : getServer().getOnlinePlayers()) {
                if (securityManager.isAuthenticated(onlinePlayer)) {
                    futures.add(inventorySyncManager.saveInventory(onlinePlayer));
                }
            }
            if (!futures.isEmpty()) {
                getLogger().info("Saving " + futures.size() + " online players' inventories before shutdown...");
                try {
                    java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0])).join();
                    getLogger().info("All online players' inventories saved successfully.");
                } catch (Exception e) {
                    getLogger().severe("Failed to save online players' inventories on shutdown: " + e.getMessage());
                }
            }

            // Wait for all active background saves (e.g. from players kicked/quit during shutdown)
            getLogger().info("Waiting for all active background saves to complete...");
            inventorySyncManager.waitForActiveSaves();
            getLogger().info("All background saves completed.");
        }

        if (wailaTask != null) {
            wailaTask.cleanup();
        }
        if (wirelessNetworkRegistry != null) {
            wirelessNetworkRegistry.clear();
        }
        if (addonManager != null) {
            addonManager.shutdown();
        }
        if (gridManager != null) {
            gridManager.shutdown();
        }
        if (taskEngine != null) {
            taskEngine.shutdown();
        }
        if (storageManager != null) {
            storageManager.shutdown();
        }
        if (permissionManager != null) {
            permissionManager.unregisterPermissions();
        }

        CommandAPI.onDisable();
    }

    public static VortexiaCore getInstance() {
        return instance;
    }

    public LoggerService getLoggerService() {
        return loggerService;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public IdentityUtil getIdentityUtil() {
        return identityUtil;
    }

    public IdentityMigrationHelper getIdentityMigrationHelper() {
        return identityMigrationHelper;
    }

    public CitizenCardManager getCitizenCardManager() {
        return citizenCardManager;
    }

    public SecurityGUI getSecurityGUI() {
        return securityGUI;
    }

    public SecurityManager getSecurityManager() {
        return securityManager;
    }

    public InventorySyncManager getInventorySyncManager() {
        return inventorySyncManager;
    }

    public AuthHookManager getAuthHookManager() {
        return authHookManager;
    }

    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    public CoreAddonManager getAddonManager() {
        return addonManager;
    }

    public SchedulerService getSchedulerService() {
        return schedulerService;
    }

    public ProxySyncService getProxySyncService() {
        return proxySyncService;
    }

    public VortexiaTaskEngine getTaskEngine() {
        return taskEngine;
    }

    public CoreItemRegistry getItemRegistry() {
        return itemRegistry;
    }

    public GuideGUI getGuideGUI() {
        return guideGUI;
    }

    public CoreWirelessNetworkRegistry getWirelessRegistry() {
        return wirelessNetworkRegistry;
    }

    public CoreWailaManager getWailaManager() {
        return wailaManager;
    }

    public CoreGridManager getGridManager() {
        return gridManager;
    }

    public CoreCustomRecipeManager getCustomRecipeManager() {
        return customRecipeManager;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }
}
