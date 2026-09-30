// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.core.network.wireless;

import me.alikuxac.vortexia.api.network.wireless.WirelessChannelInfo;

public class CoreWirelessChannelInfo implements WirelessChannelInfo {

    private final String frequency;
    private final double maxThroughput;
    private final double speedMultiplier;
    private final int activeInputsCount;
    private final int activeOutputsCount;
    private final int activeSupportsCount;

    public CoreWirelessChannelInfo(String frequency, double maxThroughput, double speedMultiplier,
                                   int activeInputsCount, int activeOutputsCount, int activeSupportsCount) {
        this.frequency = frequency;
        this.maxThroughput = maxThroughput;
        this.speedMultiplier = speedMultiplier;
        this.activeInputsCount = activeInputsCount;
        this.activeOutputsCount = activeOutputsCount;
        this.activeSupportsCount = activeSupportsCount;
    }

    @Override
    public String getFrequency() {
        return frequency;
    }

    @Override
    public double getMaxThroughput() {
        return maxThroughput;
    }

    @Override
    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    @Override
    public int getActiveInputsCount() {
        return activeInputsCount;
    }

    @Override
    public int getActiveOutputsCount() {
        return activeOutputsCount;
    }

    @Override
    public int getActiveSupportsCount() {
        return activeSupportsCount;
    }
}
