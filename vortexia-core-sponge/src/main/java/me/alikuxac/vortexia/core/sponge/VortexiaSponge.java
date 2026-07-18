// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.sponge;

import com.google.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.plugin.builtin.jvm.Plugin;

@Plugin("vortexiacore")
public class VortexiaSponge {

    private final Logger logger;

    @Inject
    public VortexiaSponge(Logger logger) {
        this.logger = logger;
    }

    @Listener
    public void onServerStart(StartedEngineEvent<org.spongepowered.api.Engine> event) {
        logger.info("VortexiaCore support (Sponge) enabled successfully!");
    }
}
