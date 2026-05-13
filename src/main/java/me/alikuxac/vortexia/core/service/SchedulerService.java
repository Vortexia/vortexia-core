// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.service;

import me.alikuxac.vortexia.core.VortexiaCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

public class SchedulerService {

    private final VortexiaCore plugin;
    private final boolean isFolia;

    public SchedulerService(VortexiaCore plugin) {
        this.plugin = plugin;
        this.isFolia = checkFolia();
    }

    private boolean checkFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregionsupport.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public boolean isFolia() {
        return isFolia;
    }

    /**
     * Run a task on the global region (Folia) or globally (Bukkit)
     */
    public void runGlobal(Runnable runnable) {
        if (isFolia) {
            Bukkit.getGlobalRegionScheduler().execute(plugin, runnable);
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    /**
     * Run a task on the region of a specific entity
     */
    public void runEntity(Entity entity, Runnable runnable) {
        if (isFolia) {
            entity.getScheduler().execute(plugin, runnable, null, 1L);
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    /**
     * Run a task delayed on the global region
     */
    public void runDelayed(Runnable runnable, long ticks) {
        if (isFolia) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> runnable.run(), ticks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, ticks);
        }
    }

    /**
     * Run a task delayed on the region of an entity
     */
    public void runEntityDelayed(Entity entity, Runnable runnable, long ticks) {
        if (isFolia) {
            entity.getScheduler().runDelayed(plugin, t -> runnable.run(), null, ticks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, ticks);
        }
    }

    /**
     * Run a task asynchronously
     */
    public void runAsync(Runnable runnable) {
        if (isFolia) {
            Bukkit.getAsyncScheduler().runNow(plugin, t -> runnable.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
    }
}
