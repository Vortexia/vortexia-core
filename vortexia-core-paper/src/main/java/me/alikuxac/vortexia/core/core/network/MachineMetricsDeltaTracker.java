// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.core.network;

import me.alikuxac.vortexia.api.network.protocol.MachineSyncPacket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache tracking previous tick machine metrics & delta thresholding.
 */
public class MachineMetricsDeltaTracker {

    private static final int ENERGY_THRESHOLD = 5;
    private static final int TEMPERATURE_THRESHOLD = 1;
    private static final byte STATE_CHANGE_THRESHOLD = 1;

    private final Map<Long, MachineSyncPacket> lastEmittedState = new ConcurrentHashMap<>();

    /**
     * Checks if the metrics surpassed defined delta thresholds.
     * Returns true if packet should be emitted.
     */
    public boolean shouldEmit(long blockPos, MachineSyncPacket currentPacket) {
        if (currentPacket == null) return false;

        MachineSyncPacket previous = lastEmittedState.get(blockPos);
        if (previous == null) {
            lastEmittedState.put(blockPos, currentPacket);
            return true;
        }

        boolean stateChanged = previous.state() != currentPacket.state();
        boolean energySurpassed = Math.abs(previous.currentEnergy() - currentPacket.currentEnergy()) >= ENERGY_THRESHOLD;

        if (stateChanged || energySurpassed) {
            lastEmittedState.put(blockPos, currentPacket);
            return true;
        }

        return false;
    }

    /**
     * Clears cached state for a block position when destroyed/removed.
     */
    public void remove(long blockPos) {
        lastEmittedState.remove(blockPos);
    }

    /**
     * Clears all cached metric states.
     */
    public void clear() {
        lastEmittedState.clear();
    }
}
