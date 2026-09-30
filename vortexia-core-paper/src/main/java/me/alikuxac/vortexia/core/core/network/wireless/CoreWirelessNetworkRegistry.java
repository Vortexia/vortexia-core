// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.core.network.wireless;

import me.alikuxac.vortexia.api.network.wireless.*;
import org.bukkit.Location;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CoreWirelessNetworkRegistry implements WirelessNetworkRegistry {

    private final Map<String, Set<WirelessNode>> frequencyNodes = new ConcurrentHashMap<>();

    @Override
    public void registerNode(WirelessNode node) {
        if (node == null || node.getFrequency() == null) return;
        frequencyNodes.computeIfAbsent(node.getFrequency(), k -> ConcurrentHashMap.newKeySet()).add(node);
    }

    @Override
    public void unregisterNode(WirelessNode node) {
        if (node == null || node.getFrequency() == null) return;
        Set<WirelessNode> nodes = frequencyNodes.get(node.getFrequency());
        if (nodes != null) {
            nodes.remove(node);
            if (nodes.isEmpty()) {
                frequencyNodes.remove(node.getFrequency());
            }
        }
    }

    @Override
    public Collection<WirelessNode> getNodesByFrequency(String frequency) {
        if (frequency == null) return Collections.emptyList();
        Set<WirelessNode> nodes = frequencyNodes.get(frequency);
        return nodes != null ? Collections.unmodifiableCollection(nodes) : Collections.emptyList();
    }

    public WirelessNode getWirelessNodeAt(Location loc) {
        if (loc == null) return null;
        for (Set<WirelessNode> set : frequencyNodes.values()) {
            for (WirelessNode node : set) {
                if (node.getLocation() != null && node.getLocation().equals(loc)) {
                    return node;
                }
            }
        }
        return null;
    }

    @Override
    public Optional<WirelessChannelInfo> getChannelInfo(String frequency) {
        if (frequency == null) return Optional.empty();
        Collection<WirelessNode> nodes = getNodesByFrequency(frequency);
        if (nodes.isEmpty()) return Optional.empty();

        double maxThroughput = 100.0; // Default base throughput
        double speedMultiplier = 1.0;  // Default base speed

        int inputs = 0;
        int outputs = 0;
        int supports = 0;

        for (WirelessNode node : nodes) {
            if (node.getRole() == null) continue;
            switch (node.getRole()) {
                case INPUT -> inputs++;
                case OUTPUT -> outputs++;
                case SUPPORT -> {
                    supports++;
                    if (node instanceof WirelessSupportNode sNode) {
                        maxThroughput += sNode.getThroughputBonus();
                        speedMultiplier += sNode.getSpeedBonus();
                    }
                }
            }
        }

        return Optional.of(new CoreWirelessChannelInfo(frequency, maxThroughput, speedMultiplier, inputs, outputs, supports));
    }

    @Override
    public double requestTransfer(String frequency, double amount) {
        if (frequency == null || amount <= 0) return 0;

        Collection<WirelessNode> nodes = getNodesByFrequency(frequency);
        if (nodes.isEmpty()) return 0;

        // Calculate wireless channel limitations
        Optional<WirelessChannelInfo> infoOpt = getChannelInfo(frequency);
        if (infoOpt.isEmpty()) return 0;
        WirelessChannelInfo info = infoOpt.get();

        int activeOutputs = info.getActiveOutputsCount();
        if (activeOutputs == 0) return 0;

        // Limit transfer amount by max channel throughput
        double allowedAmount = Math.min(amount, info.getMaxThroughput());
        if (allowedAmount <= 0) return 0;

        // Gather output nodes (OUTPUT)
        List<WirelessNode> outputNodes = new ArrayList<>();
        for (WirelessNode node : nodes) {
            if (node.getRole() == WirelessRole.OUTPUT) {
                outputNodes.add(node);
            }
        }

        if (outputNodes.isEmpty()) return 0;

        // Distribute resource evenly among output nodes
        double shareAmount = allowedAmount / outputNodes.size();
        double actualTransferred = 0;

        for (WirelessNode output : outputNodes) {
            actualTransferred += output.acceptResource(shareAmount);
        }

        return actualTransferred;
    }

    public void clear() {
        frequencyNodes.clear();
    }
}
