// Developed by alikuxac - Project Vortexia
package me.alikuxac.vortexia.core.core.grid;

import me.alikuxac.vortexia.api.grid.Grid;
import me.alikuxac.vortexia.api.grid.GridNode;
import me.alikuxac.vortexia.api.grid.GridSolver;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementation of the physical network Grid interface in Vortexia Core.
 */
public class CoreGrid implements Grid {

    private final UUID id;
    private final String networkType;
    private final List<GridNode> nodes = new CopyOnWriteArrayList<>();
    private final GridSolver solver;

    public CoreGrid(UUID id, String networkType, GridSolver solver) {
        this.id = id;
        this.networkType = networkType;
        this.solver = solver;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public String getNetworkType() {
        return networkType;
    }

    @Override
    public Collection<GridNode> getNodes() {
        return Collections.unmodifiableCollection(nodes);
    }

    @Override
    public void addNode(GridNode node) {
        if (node != null && !nodes.contains(node)) {
            nodes.add(node);
            node.setGrid(this);
        }
    }

    @Override
    public void removeNode(GridNode node) {
        if (node != null) {
            nodes.remove(node);
            if (node.getGrid() == this) {
                node.setGrid(null);
            }
        }
    }

    @Override
    public void tick() {
        if (solver != null && !nodes.isEmpty()) {
            solver.solve(this);
        }
    }
}
