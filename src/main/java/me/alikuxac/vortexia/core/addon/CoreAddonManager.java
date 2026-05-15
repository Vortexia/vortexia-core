// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.addon;

import me.alikuxac.vortexia.api.addon.AddonManager;
import me.alikuxac.vortexia.api.addon.VortexiaAddon;
import me.alikuxac.vortexia.core.VortexiaCore;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CoreAddonManager implements AddonManager {

    private final VortexiaCore plugin;
    private final Map<String, VortexiaAddon> addons = new ConcurrentHashMap<>();

    public CoreAddonManager(VortexiaCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void registerAddon(VortexiaAddon addon) {
        if (addons.containsKey(addon.getAddonName())) {
            plugin.getLogger().warning("Addon " + addon.getAddonName() + " is already registered.");
            return;
        }

        addons.put(addon.getAddonName(), addon);
        try {
            addon.onAddonEnable();
            plugin.getLogger().info("Successfully registered Addon: " + addon.getAddonName() + " v" + addon.getVersion());
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to enable Addon: " + addon.getAddonName());
            e.printStackTrace();
        }
    }

    @Override
    public void unregisterAddon(VortexiaAddon addon) {
        unregisterAddon(addon.getAddonName());
    }

    @Override
    public void unregisterAddon(String name) {
        VortexiaAddon addon = addons.remove(name);
        if (addon != null) {
            try {
                addon.onAddonDisable();
                plugin.getLogger().info("Unregistered Addon: " + name);
            } catch (Exception e) {
                plugin.getLogger().severe("Error while disabling Addon: " + name);
                e.printStackTrace();
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends VortexiaAddon> T getAddon(String name) {
        return (T) addons.get(name);
    }

    @Override
    public Collection<VortexiaAddon> getAddons() {
        return Collections.unmodifiableCollection(addons.values());
    }

    public void shutdown() {
        for (VortexiaAddon addon : addons.values()) {
            unregisterAddon(addon.getAddonName());
        }
    }
}
