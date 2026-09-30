// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.utils;

import me.alikuxac.vortexia.core.backend.utils.CommonUpdateChecker;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class UpdateChecker {

    private final CommonUpdateChecker commonChecker;

    public UpdateChecker(String currentVersion, String currentLoader,
                         Consumer<String> infoLogger, Consumer<String> warnLogger, Consumer<String> debugLogger,
                         Consumer<Runnable> asyncExecutor) {
        this.commonChecker = new CommonUpdateChecker(currentVersion, currentLoader, infoLogger, warnLogger, debugLogger, asyncExecutor);
    }

    public CompletableFuture<Void> checkAsync() {
        return commonChecker.checkAsync();
    }

    public boolean isUpdateAvailable() {
        return commonChecker.isUpdateAvailable();
    }

    public boolean hasChecked() {
        return commonChecker.hasChecked();
    }

    public String getCurrentVersion() {
        return commonChecker.getCurrentVersion();
    }

    public String getLatestVersion() {
        return commonChecker.getLatestVersion();
    }

    public String getUpdateMessage() {
        return commonChecker.getUpdateMessage();
    }
}
