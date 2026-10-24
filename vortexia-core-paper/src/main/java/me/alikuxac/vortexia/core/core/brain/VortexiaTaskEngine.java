// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.core.brain;

import me.alikuxac.vortexia.core.VortexiaCore;
import me.alikuxac.vortexia.api.scheduler.TaskEngine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Universal Task Engine (The Brain).
 * Responsible for coordinating all addon-specific tasks (machine ticking, grid solving, etc.)
 */
public class VortexiaTaskEngine implements TaskEngine {

    private final VortexiaCore plugin;
    private final Map<String, Runnable> recurringTasks = new ConcurrentHashMap<>();
    private final Map<String, Runnable> registeredProcessors = new ConcurrentHashMap<>();

    public VortexiaTaskEngine(VortexiaCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers a recurring processor for an addon.
     * @param id Unique ID for the processor (e.g. "vitality:machine_tick")
     * @param processor The logic to run
     * @param period Period in ticks
     */
    public void registerRecurringProcessor(String id, Runnable processor, long period) {
        Runnable existing = recurringTasks.remove(id);
        if (existing != null) {
            existing.run();
        }

        Runnable canceller = plugin.getSchedulerService().runRepeating(processor, period);
        recurringTasks.put(id, canceller);
        registeredProcessors.put(id, processor);

        plugin.getLogger().info("Brain Engine: Registered recurring processor [" + id + "] every " + period + " ticks.");
    }

    /**
     * Runs a task asynchronously for heavy calculations.
     * @param task The task to run
     */
    public void runAsync(Runnable task) {
        plugin.getSchedulerService().runAsync(task);
    }

    /**
     * Stops all registered tasks.
     */
    public void shutdown() {
        recurringTasks.values().forEach(Runnable::run);
        recurringTasks.clear();
        registeredProcessors.clear();
        plugin.getLogger().info("Brain Engine: All processors shut down.");
    }

    public void unregisterProcessor(String id) {
        Runnable canceller = recurringTasks.remove(id);
        if (canceller != null) {
            canceller.run();
            registeredProcessors.remove(id);
            plugin.getLogger().info("Brain Engine: Unregistered processor [" + id + "]");
        }
    }
}
