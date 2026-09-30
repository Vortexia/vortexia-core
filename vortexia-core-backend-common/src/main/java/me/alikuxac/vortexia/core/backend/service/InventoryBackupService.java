// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.backend.service;

import java.util.UUID;

public class InventoryBackupService {

    private final int snapshotLimit;
    private final boolean enabled;

    public InventoryBackupService(int snapshotLimit, boolean enabled) {
        this.snapshotLimit = Math.max(1, snapshotLimit);
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getSnapshotLimit() {
        return snapshotLimit;
    }

    public boolean shouldCleanSnapshots(int currentSnapshotCount) {
        return currentSnapshotCount > snapshotLimit;
    }

    public boolean canSaveInventory(boolean isAuthenticated) {
        return enabled && isAuthenticated;
    }
}
