package com.vortexia.core;

import com.vortexia.core.command.CommandManager;

import com.vortexia.core.config.ConfigManager;
import com.vortexia.core.service.LoggerService;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIPaperConfig;
import org.bukkit.plugin.java.JavaPlugin;

public final class VortexiaCore extends JavaPlugin {

    private static VortexiaCore instance;
    private LoggerService loggerService;
    private ConfigManager configManager;
    private CommandManager commandManager;

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
        // Load configuration
        this.configManager = new ConfigManager(this);
        this.configManager.loadConfig();

        // Initialize Logger
        this.loggerService = new LoggerService(this);

        // Setup Commands
        CommandAPI.onEnable();
        this.commandManager = new CommandManager(this);
        this.commandManager.registerCommands();
    }

    @Override
    public void onDisable() {
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
}
