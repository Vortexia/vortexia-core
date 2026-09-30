// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.sponge;

import com.google.inject.Inject;
import me.alikuxac.vortexia.core.backend.utils.CommonUpdateChecker;
import me.alikuxac.vortexia.core.sponge.listener.SpongeAuthRestrictListener;
import me.alikuxac.vortexia.core.sponge.listener.SpongePlayerListener;
import me.alikuxac.vortexia.core.sponge.service.SpongeInventorySyncManager;
import me.alikuxac.vortexia.core.sponge.service.SpongeSecurityManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

@Plugin("vortexiacore")
public class VortexiaSponge {

    private final Logger logger;
    private final PluginContainer container;
    private SpongeInventorySyncManager inventorySyncManager;
    private SpongeSecurityManager securityManager;
    private CommonUpdateChecker updateChecker;
    private boolean inventorySyncEnabled = true;

    @Inject
    public VortexiaSponge(Logger logger, PluginContainer container) {
        this.logger = logger;
        this.container = container;
    }

    @Listener
    public void onServerStart(StartedEngineEvent<org.spongepowered.api.Engine> event) {
        this.inventorySyncManager = new SpongeInventorySyncManager(this);
        this.securityManager = new SpongeSecurityManager(this);
        
        // Register listeners
        Sponge.eventManager().registerListeners(container, new SpongePlayerListener(this));
        Sponge.eventManager().registerListeners(container, new SpongeAuthRestrictListener(this));

        // Start Update Checker
        String currentVersion = container.metadata().version().toString();
        this.updateChecker = new CommonUpdateChecker(
            currentVersion,
            "sponge",
            msg -> logger.info(msg),
            msg -> logger.warn(msg),
            msg -> logger.debug(msg),
            runnable -> Sponge.asyncScheduler().executor(container).submit(runnable)
        );
        this.updateChecker.checkAsync();

        logger.info("VortexiaCore support (Sponge API 10) enabled successfully!");
    }

    public Logger getLogger() {
        return logger;
    }

    public SpongeInventorySyncManager getInventorySyncManager() {
        return inventorySyncManager;
    }

    public SpongeSecurityManager getSecurityManager() {
        return securityManager;
    }

    public boolean isInventorySyncEnabled() {
        return inventorySyncEnabled;
    }

    public CommonUpdateChecker getUpdateChecker() {
        return updateChecker;
    }
}
