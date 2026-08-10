// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.backend.command;

import java.util.function.Consumer;

public class CommonReloadLogic {

    private final Runnable clearCacheTask;
    private final Runnable reloadConfigTask;
    private final Consumer<String> logger;

    public CommonReloadLogic(Runnable clearCacheTask, Runnable reloadConfigTask, Consumer<String> logger) {
        this.clearCacheTask = clearCacheTask;
        this.reloadConfigTask = reloadConfigTask;
        this.logger = logger;
    }

    public void executeReload() {
        try {
            if (reloadConfigTask != null) {
                reloadConfigTask.run();
            }
            if (clearCacheTask != null) {
                clearCacheTask.run();
            }
            logger.accept("Configuration and cache reloaded successfully.");
        } catch (Exception e) {
            logger.accept("Failed to reload: " + e.getMessage());
        }
    }
}
