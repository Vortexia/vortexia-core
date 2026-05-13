// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIPaperConfig;
import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.java.JavaPlugin;

import me.alikuxac.vortexia.api.VortexiaProvider;
import me.alikuxac.vortexia.core.api.CoreVortexiaAPI;
import me.alikuxac.vortexia.core.command.CommandManager;
import me.alikuxac.vortexia.core.config.ConfigManager;
import me.alikuxac.vortexia.core.listener.PlayerListener;
import me.alikuxac.vortexia.core.service.LoggerService;
import me.alikuxac.vortexia.core.storage.StorageException;
import me.alikuxac.vortexia.core.storage.StorageManager;
import me.alikuxac.vortexia.core.storage.util.IdentityUtil;

public final class VortexiaCore extends JavaPlugin {

    private static VortexiaCore instance;
    private LoggerService loggerService;
    private ConfigManager configManager;
    private CommandManager commandManager;
    private StorageManager storageManager;
    private IdentityUtil identityUtil;
    private me.alikuxac.vortexia.core.storage.util.IdentityMigrationHelper identityMigrationHelper;
    private me.alikuxac.vortexia.core.item.CitizenCardManager citizenCardManager;
    private me.alikuxac.vortexia.core.gui.SecurityGUI securityGUI;
    private me.alikuxac.vortexia.core.service.SecurityManager securityManager;
    private me.alikuxac.vortexia.core.hook.AuthHookManager authHookManager;
    private me.alikuxac.vortexia.core.service.SchedulerService schedulerService;
    private me.alikuxac.vortexia.core.service.ProxySyncService proxySyncService;
    private me.alikuxac.vortexia.core.addon.CoreAddonManager addonManager;

    @Override
    public void onLoad() {
        instance = this;
        // CommandAPI
        try {
            CommandAPI.onLoad(new CommandAPIPaperConfig(this).silentLogs(true).verboseOutput(true));
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
        this.schedulerService = new me.alikuxac.vortexia.core.service.SchedulerService(this);
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

        this.identityMigrationHelper = new me.alikuxac.vortexia.core.storage.util.IdentityMigrationHelper(this);
        this.citizenCardManager = new me.alikuxac.vortexia.core.item.CitizenCardManager(this);
        this.securityGUI = new me.alikuxac.vortexia.core.gui.SecurityGUI(this);
        this.securityManager = new me.alikuxac.vortexia.core.service.SecurityManager(this);
        this.proxySyncService = new me.alikuxac.vortexia.core.service.ProxySyncService(this);
        this.authHookManager = new me.alikuxac.vortexia.core.hook.AuthHookManager(this);

        // Initialize PacketEvents
        PacketEvents.getAPI().init();
        PacketEvents.getAPI().getEventManager().registerListener(new me.alikuxac.vortexia.core.listener.SecurityPacketListener(this));
        
        this.addonManager = new me.alikuxac.vortexia.core.addon.CoreAddonManager(this);

        // Register Auth Hooks
        if (getServer().getPluginManager().getPlugin("AuthMe") != null) {
            me.alikuxac.vortexia.core.hook.impl.AuthMeHook authMeHook = new me.alikuxac.vortexia.core.hook.impl.AuthMeHook(this);
            authMeHook.register();
            this.authHookManager.registerHook(authMeHook);
        }

        loggerService.info("Server online mode: " + (identityUtil.isOnlineMode() ? "ENABLED" : "DISABLED"));

        VortexiaProvider.register(new CoreVortexiaAPI(this));

        getServer().getPluginManager().registerEvents(
                new PlayerListener(this),
                this);
        getServer().getPluginManager().registerEvents(
                new me.alikuxac.vortexia.core.listener.CitizenCardListener(this),
                this);
        getServer().getPluginManager().registerEvents(
                new me.alikuxac.vortexia.core.listener.AuthRestrictListener(this),
                this);

        CommandAPI.onEnable();
        this.commandManager = new CommandManager(this);
        this.commandManager.registerCommands();
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
        if (addonManager != null) {
            addonManager.shutdown();
        }
        if (storageManager != null) {
            storageManager.shutdown();
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

    public me.alikuxac.vortexia.core.storage.util.IdentityMigrationHelper getIdentityMigrationHelper() {
        return identityMigrationHelper;
    }

    public me.alikuxac.vortexia.core.item.CitizenCardManager getCitizenCardManager() {
        return citizenCardManager;
    }

    public me.alikuxac.vortexia.core.gui.SecurityGUI getSecurityGUI() {
        return securityGUI;
    }

    public me.alikuxac.vortexia.core.service.SecurityManager getSecurityManager() {
        return securityManager;
    }

    public me.alikuxac.vortexia.core.hook.AuthHookManager getAuthHookManager() {
        return authHookManager;
    }

    public me.alikuxac.vortexia.core.addon.CoreAddonManager getAddonManager() {
        return addonManager;
    }

    public me.alikuxac.vortexia.core.service.SchedulerService getSchedulerService() {
        return schedulerService;
    }

    public me.alikuxac.vortexia.core.service.ProxySyncService getProxySyncService() {
        return proxySyncService;
    }
}
