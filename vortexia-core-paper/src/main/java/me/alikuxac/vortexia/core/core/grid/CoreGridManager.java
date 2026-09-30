// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.core.grid;

import me.alikuxac.vortexia.api.grid.Grid;
import me.alikuxac.vortexia.api.grid.GridNode;
import me.alikuxac.vortexia.api.grid.GridSolver;
import me.alikuxac.vortexia.api.grid.GridManager;
import me.alikuxac.vortexia.core.VortexiaCore;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementation of GridManager in Vortexia Core.
 * Handles the graph merge/split algorithms and ticks all active grids.
 */
public class CoreGridManager implements GridManager {

    private final VortexiaCore plugin;
    
    // Map of active grids grouped by networkType
    private final Map<String, List<Grid>> activeGrids = new ConcurrentHashMap<>();
    
    // Quick lookup of registered nodes by their block Location
    private final Map<Location, GridNode> registeredNodes = new ConcurrentHashMap<>();
    
    // Solvers registered for network types
    private final Map<String, GridSolver> solvers = new ConcurrentHashMap<>();

    private static final BlockFace[] CARDINAL_FACES = {
        BlockFace.UP, BlockFace.DOWN,
        BlockFace.NORTH, BlockFace.SOUTH,
        BlockFace.EAST, BlockFace.WEST
    };

    public CoreGridManager(VortexiaCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Starts the periodic tick propagation processor.
     */
    public void startTicking() {
        plugin.getTaskEngine().registerRecurringProcessor("core:grid_propagation", this::tickAllGrids, 1L);
        plugin.getLogger().info("Grid Manager: Grid Tick Processor registered successfully.");
    }

    /**
     * Stops and cleans up all active grids.
     */
    public void shutdown() {
        plugin.getTaskEngine().unregisterProcessor("core:grid_propagation");
        activeGrids.clear();
        registeredNodes.clear();
        solvers.clear();
        plugin.getLogger().info("Grid Manager: Shut down all active grids and solvers.");
    }

    private void tickAllGrids() {
        for (List<Grid> grids : activeGrids.values()) {
            for (Grid grid : grids) {
                try {
                    grid.tick();
                } catch (Exception e) {
                    plugin.getLogger().severe("Grid Manager: Error ticking grid " + grid.getId() + " (" + grid.getNetworkType() + ")");
                    e.printStackTrace();
                }
            }
        }
    }

    private Location normalizeLocation(Location raw) {
        if (raw == null) return null;
        return new Location(raw.getWorld(), raw.getBlockX(), raw.getBlockY(), raw.getBlockZ());
    }

    @Override

    public synchronized void registerNode(GridNode node) {
        if (node == null || node.getLocation() == null) return;

        Location loc = normalizeLocation(node.getLocation()); // Normalize location to block-grid
        String networkType = node.getNetworkType();

        // Prevent duplicate registration
        if (registeredNodes.containsKey(loc)) {
            unregisterNode(loc, networkType);
        }

        registeredNodes.put(loc, node);

        // Find distinct adjacent grids of the same network type
        Set<Grid> adjacentGrids = new HashSet<>();
        for (BlockFace face : CARDINAL_FACES) {
            Location neighborLoc = loc.clone().add(face.getModX(), face.getModY(), face.getModZ());
            GridNode neighborNode = registeredNodes.get(neighborLoc);
            if (neighborNode != null && neighborNode.getNetworkType().equals(networkType)) {
                Grid neighborGrid = neighborNode.getGrid();
                if (neighborGrid != null) {
                    adjacentGrids.add(neighborGrid);
                }
            }
        }

        GridSolver solver = solvers.get(networkType);

        if (adjacentGrids.isEmpty()) {
            // Case 1: No adjacent grids. Create a brand new grid.
            Grid newGrid = new CoreGrid(UUID.randomUUID(), networkType, solver);
            newGrid.addNode(node);
            
            activeGrids.computeIfAbsent(networkType, k -> new CopyOnWriteArrayList<>()).add(newGrid);
            plugin.getLogger().fine("Grid Manager: Created new grid [" + newGrid.getId() + "] for type " + networkType);
        } else if (adjacentGrids.size() == 1) {
            // Case 2: Exactly 1 adjacent grid. Add this node to it.
            Grid existingGrid = adjacentGrids.iterator().next();
            existingGrid.addNode(node);
            plugin.getLogger().fine("Grid Manager: Added node to existing grid [" + existingGrid.getId() + "]");
        } else {
            // Case 3: More than 1 adjacent grid. Merge all of them into a single surviving grid.
            Iterator<Grid> iterator = adjacentGrids.iterator();
            Grid survivingGrid = iterator.next();
            survivingGrid.addNode(node);

            List<Grid> typeGrids = activeGrids.get(networkType);

            while (iterator.hasNext()) {
                Grid obsoleteGrid = iterator.next();
                plugin.getLogger().fine("Grid Manager: Merging grid [" + obsoleteGrid.getId() + "] into [" + survivingGrid.getId() + "]");
                
                // Move all nodes from obsolete grid to surviving grid
                Collection<GridNode> obsoleteNodes = new ArrayList<>(obsoleteGrid.getNodes());
                for (GridNode oNode : obsoleteNodes) {
                    obsoleteGrid.removeNode(oNode);
                    survivingGrid.addNode(oNode);
                }

                // Remove the empty obsolete grid
                if (typeGrids != null) {
                    typeGrids.remove(obsoleteGrid);
                }
            }
        }
    }

    @Override
    public synchronized void unregisterNode(Location location, String networkType) {
        if (location == null || networkType == null) return;

        Location loc = normalizeLocation(location); // Normalize
        GridNode node = registeredNodes.remove(loc);
        if (node == null) return;

        Grid grid = node.getGrid();
        if (grid == null) return;

        grid.removeNode(node);

        List<Grid> typeGrids = activeGrids.get(networkType);

        if (grid.getNodes().isEmpty()) {
            // Grid is empty, clean it up
            if (typeGrids != null) {
                typeGrids.remove(grid);
            }
            plugin.getLogger().fine("Grid Manager: Removed empty grid [" + grid.getId() + "]");
            return;
        }

        // Run Split Algorithm (BFS traversal to find isolated components)
        Set<GridNode> remainingNodes = new HashSet<>(grid.getNodes());
        List<Set<GridNode>> components = new ArrayList<>();

        while (!remainingNodes.isEmpty()) {
            GridNode startNode = remainingNodes.iterator().next();
            Set<GridNode> component = new HashSet<>();
            Queue<GridNode> queue = new LinkedList<>();

            queue.add(startNode);
            component.add(startNode);

            while (!queue.isEmpty()) {
                GridNode current = queue.poll();
                Location currentLoc = normalizeLocation(current.getLocation());

                for (BlockFace face : CARDINAL_FACES) {
                    Location neighborLoc = currentLoc.clone().add(face.getModX(), face.getModY(), face.getModZ());
                    GridNode neighborNode = registeredNodes.get(neighborLoc);
                    
                    if (neighborNode != null && neighborNode.getNetworkType().equals(networkType)) {
                        if (remainingNodes.contains(neighborNode) && !component.contains(neighborNode)) {
                            component.add(neighborNode);
                            queue.add(neighborNode);
                        }
                    }
                }
            }

            components.add(component);
            remainingNodes.removeAll(component);
        }

        if (components.size() > 1) {
            plugin.getLogger().fine("Grid Manager: Split detected! Breaking grid [" + grid.getId() + "] into " + components.size() + " separate grids.");
            
            GridSolver solver = solvers.get(networkType);

            // First component stays in the current grid, others are moved to new grids
            for (int i = 1; i < components.size(); i++) {
                Set<GridNode> splitComponent = components.get(i);
                Grid newGrid = new CoreGrid(UUID.randomUUID(), networkType, solver);

                for (GridNode sNode : splitComponent) {
                    grid.removeNode(sNode); // Remove from old grid
                    newGrid.addNode(sNode); // Add to new grid
                }

                if (typeGrids != null) {
                    typeGrids.add(newGrid);
                }
            }
        }
    }

    @Override
    public Collection<Grid> getGrids(String networkType) {
        List<Grid> list = activeGrids.get(networkType);
        return list != null ? Collections.unmodifiableCollection(list) : Collections.emptyList();
    }

    @Override
    public Grid getGridAt(Location location, String networkType) {
        if (location == null) return null;
        GridNode node = registeredNodes.get(normalizeLocation(location));
        if (node != null && node.getNetworkType().equals(networkType)) {
            return node.getGrid();
        }
        return null;
    }

    @Override
    public void registerSolver(String networkType, GridSolver solver) {
        if (networkType == null || solver == null) return;
        solvers.put(networkType, solver);
        plugin.getLogger().info("Grid Manager: Registered grid solver for type [" + networkType + "]");

        // Apply solver to all active grids of this type retrospectively
        List<Grid> list = activeGrids.get(networkType);
        if (list != null) {
            for (Grid g : list) {
                if (g instanceof CoreGrid cg) {
                    // Update solver
                    activeGrids.get(networkType).remove(g);
                    CoreGrid newG = new CoreGrid(cg.getId(), cg.getNetworkType(), solver);
                    for (GridNode node : cg.getNodes()) {
                        newG.addNode(node);
                    }
                    activeGrids.get(networkType).add(newG);
                }
            }
        }
    }
}
