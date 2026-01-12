// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIPaperConfig;
import org.bukkit.plugin.java.JavaPlugin;

import me.alikuxac.vortexia.core.api.VortexiaAPI;
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

    @Override
    public void onLoad() {
        instance = this;
        try {
            CommandAPI.onLoad(new CommandAPIPaperConfig(this).silentLogs(true).verboseOutput(true));
        } catch (Exception e) {
            getLogger().severe("VortexiaCore - Failed to load CommandAPI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.configManager.loadConfig();

        this.loggerService = new LoggerService(this);
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

        loggerService.info("Server online mode: " + (identityUtil.isOnlineMode() ? "ENABLED" : "DISABLED"));

        VortexiaAPI.initialize(this);

        getServer().getPluginManager().registerEvents(
                new PlayerListener(this),
                this);

        CommandAPI.onEnable();
        this.commandManager = new CommandManager(this);
        this.commandManager.registerCommands();
    }

    @Override
    public void onDisable() {
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
}
