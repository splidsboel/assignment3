package ch;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class Graph {
    int n, m;

    public static class Vertex {
        float x, y;

        public Vertex(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    public class Edge {
        long to;
        int weight;
        long contracted; // only used by contraction hierachy, marks the vertex from which this edge resulted.

        public Edge(long to, int weight, long contracted) {
            this.to = to;
            this.weight = weight;
            this.contracted = contracted;
        }
    }

    public static class Shortcut {
        public final long from;
        public final long to;
        public final int weight;
        public final long via;

        public Shortcut(long from, long to, int weight, long via) {
            this.from = from;
            this.to = to;
            this.weight = weight;
            this.via = via;
        }
    }

    public static class ContractResult {
        public final int shortcutsAdded;
        public final List<Shortcut> shortcuts;

        public ContractResult(int shortcutsAdded, List<Shortcut> shortcuts) {
            this.shortcutsAdded = shortcutsAdded;
            this.shortcuts = shortcuts;
        }
    }

    private Map<Long, List<Edge>> edges;
    private Map<Long, Vertex> vertices;

    public Graph() {
        this.n = 0;
        this.m = 0;
        this.edges = new HashMap<>();
        this.vertices = new HashMap<>();
    }

    public void addVertex(long id, Vertex v) {
        this.vertices.put(id, v);
        this.n++;
    }

    public void addEdge(long from, long to, long contracted, int weight) {
        if (!this.edges.containsKey(from)) {
            this.edges.put(from, new ArrayList<>());
        }
        this.edges.get(from).add(new Edge(to, weight, contracted));
        this.m++;
    }

    public void addUndirectedEdge(long u, long v, long contracted, int weight) {
        addEdge(u, v, contracted, weight);
        addEdge(v, u, contracted, weight);
    }

    public void addUndirectedEdge(long u, long v, int weight) {
        addUndirectedEdge(u, v, -1, weight);
    }

    public List<Edge> getNeighbours(long u) {
        return this.edges.get(u);
    }

    public Vertex getVertex(long id) {
        return this.vertices.get(id);
    }

    public int degree(long v) {
        return this.edges.get(v).size();
    }

    public boolean containsVertex(long id) {
        return this.vertices.containsKey(id);
    }

    public Set<Long> getVertexIds() {
        return new HashSet<>(this.vertices.keySet());
    }

    public Graph copy() {
        Graph copy = new Graph();
        for (Map.Entry<Long, Vertex> entry : this.vertices.entrySet()) {
            Vertex v = entry.getValue();
            copy.addVertex(entry.getKey(), new Vertex(v.x, v.y));
        }

        for (Map.Entry<Long, List<Edge>> entry : this.edges.entrySet()) {
            long from = entry.getKey();
            List<Edge> adjacency = entry.getValue();
            if (adjacency == null) {
                continue;
            }
            for (Edge edge : adjacency) {
                copy.addEdge(from, edge.to, edge.contracted, edge.weight);
            }
        }
        return copy;
    }

    public ContractResult contract(long v) {
        // Collect outgoing edges from v (needed after we remove v).
        List<Edge> outgoing = this.edges.get(v);
        List<Edge> outgoingCopy = outgoing == null ? new ArrayList<>() : new ArrayList<>(outgoing);

        // Collect incoming edges (edges whose head is v) and remove them.
        List<Map.Entry<Long, Integer>> incoming = new ArrayList<>();
        for (Map.Entry<Long, List<Edge>> entry : this.edges.entrySet()) {
            long from = entry.getKey();
            if (from == v) {
                continue;
            }

            List<Edge> adj = entry.getValue();
            if (adj == null || adj.isEmpty()) {
                continue;
            }

            for (int i = adj.size() - 1; i >= 0; i--) {
                Edge edge = adj.get(i);
                if (edge.to == v) {
                    incoming.add(new AbstractMap.SimpleEntry<>(from, edge.weight));
                    adj.remove(i);
                    this.m--;
                }
            }
        }

        // Remove outgoing edges from v.
        if (outgoing != null) {
            this.edges.remove(v);
            this.m -= outgoingCopy.size();
        }

        // Remove vertex v from vertex map.
        if (this.vertices.containsKey(v)) {
            this.vertices.remove(v);
            this.n--;
        }

        if (incoming.isEmpty() || outgoingCopy.isEmpty()) {
            return new ContractResult(0, Collections.emptyList());
        }

        int shortcutsAdded = 0;
        List<Shortcut> newShortcuts = new ArrayList<>();
        for (Map.Entry<Long, Integer> inEdge : incoming) {
            long u = inEdge.getKey();
            int weightUV = inEdge.getValue();

            for (Edge outEdge : outgoingCopy) {
                long w = outEdge.to;
                if (w == v) {
                    continue;
                }
                if (u == w) {
                    continue;
                }

                long shortcutWeightLong = (long) weightUV + (long) outEdge.weight;
                if (shortcutWeightLong > Integer.MAX_VALUE) {
                    continue;
                }

                int shortcutWeight = (int) shortcutWeightLong;
                int existingWeight = getWeight(u, w);
                if (existingWeight <= shortcutWeight) {
                    continue;
                }

                addEdge(u, w, v, shortcutWeight);
                shortcutsAdded++;
                newShortcuts.add(new Shortcut(u, w, shortcutWeight, v));
            }
        }

        return new ContractResult(shortcutsAdded, newShortcuts);
    }

    public int getEdgeDifference(long v) {
        List<Edge> neighbours = getNeighbours(v);
        if (neighbours == null || neighbours.isEmpty()) {
            return 0;
        }

        Map<Long, Integer> neighbourWeights = new HashMap<>();
        for (Edge edge : neighbours) {
            if (edge.to == v) {
                continue;
            }
            int best = neighbourWeights.getOrDefault(edge.to, Integer.MAX_VALUE);
            if (edge.weight < best) {
                neighbourWeights.put(edge.to, edge.weight);
            }
        }

        int removedEdges = neighbourWeights.size();
        int shortcuts = 0;
        List<Map.Entry<Long, Integer>> entries = new ArrayList<>(neighbourWeights.entrySet());

        for (int i = 0; i < entries.size(); i++) {
            long u = entries.get(i).getKey();
            int weightUV = entries.get(i).getValue();

            for (int j = i + 1; j < entries.size(); j++) {
                long w = entries.get(j).getKey();
                int weightVW = entries.get(j).getValue();

                if (u == w) {
                    continue;
                }

                long shortcutWeight = (long) weightUV + (long) weightVW;
                int forward = getWeight(u, w);
                int backward = getWeight(w, u);
                int bestExisting = Math.min(forward, backward);

                if (bestExisting == Integer.MAX_VALUE || (long) bestExisting > shortcutWeight) {
                    shortcuts++;
                }
            }
        }

        return shortcuts - removedEdges;
    }
    //helper method for getEdgeDifference()
    private int getWeight(long from, long to) {
        List<Edge> outgoing = this.edges.get(from);
        if (outgoing == null || outgoing.isEmpty()) {
            return Integer.MAX_VALUE;
        }

        int best = Integer.MAX_VALUE;
        for (Edge edge : outgoing) {
            if (edge.to == to && edge.weight < best) {
                best = edge.weight;
            }
        }
        return best;
    }

    /**
     * Entry point for computing a nested-dissection ordering for the current graph.
     * The actual work is delegated to {@link #nestedDissection(Set, List)} which operates
     * purely on vertex identifiers to avoid mutating the graph while preparing CH priorities.
     */
    public List<Long> computeNestedDissectionOrder() {
        Set<Long> allVertices = getVertexIds();
        List<Long> ordering = new ArrayList<>(allVertices.size());
        nestedDissection(allVertices, ordering);
        return ordering;
    }

    /**
     * Recursively build the nested-dissection ordering.
     *
     * @param subgraphVertices vertices that are still unassigned within the current subproblem
     * @param ordering accumulator that collects the final permutation (low recursion levels first)
     *
     * The intended implementation steps:
     * 1. Base case: when the subgraph is small enough, append the vertices directly (e.g. using
     *    plain heuristics such as edge difference) and return.
     * 2. Separator search: call {@link #findSeparator(Set)} to obtain a small balanced vertex cut.
     *    This should run a pseudo-diameter style sweep (or coordinate split) and refine it to a
     *    minimal disconnecting set.
     * 3. Partitioning: use {@link #splitBySeparator(Set, Set)} to obtain the disconnected regions
     *    that remain after removing the separator.
     * 4. Recurse on each region, ensuring that ordering stays local (process every region before
     *    finally appending the separator).
     * 5. Append the separator vertices at the end for this recursion level.
     */
    private void nestedDissection(Set<Long> subgraphVertices, List<Long> ordering) {
        // TODO: implement the base case check (e.g. subgraph size <= threshold) and append directly.

        // TODO: run the separator finder and bail out early if it fails (fallback to base-case ordering).
        Set<Long> separator = findSeparator(subgraphVertices);

        // TODO: partition the subgraph into disconnected regions after removing the separator.
        List<Set<Long>> regions = splitBySeparator(subgraphVertices, separator);

        // TODO: recurse on each region, passing a defensive copy if needed because recursion
        //       should never mutate the original graph structure.
        for (Set<Long> region : regions) {
            nestedDissection(region, ordering);
        }

        // TODO: append separator vertices now that all contained regions have been processed.
        ordering.addAll(separator);
    }

    /**
     * Locate an approximate balanced vertex separator for the provided vertex set.
     *
     * Implementation outline:
     * 1. Pick anchor vertices by running two BFS/DFS sweeps to approximate the diameter, or
     *    alternatively by splitting on the longest coordinate axis.
     * 2. Grow simultaneous frontiers from the anchors to detect the interface where the subgraph
     *    balances; store those boundary vertices as an initial separator candidate.
     * 3. Refine the candidate: remove redundant vertices that do not contribute to the cut and
     *    optionally swap very high-degree vertices for nearby low-degree alternatives.
     * 4. Verify that removing the separator disconnects the subgraph in at least two components;
     *    if it does not, restart with different anchors or enlarge the separator band.
     *
     * @param subgraphVertices vertex identifiers in the current recursive block
     * @return a set of vertices that will become the separator (never null, but may be empty)
     */
    private Set<Long> findSeparator(Set<Long> subgraphVertices) {
        // TODO: implement separator search as described above and return the resulting vertex set.
        return Collections.emptySet();
    }

    /**
     * Given a separator, split the remaining vertices into their connected regions.
     *
     * Implementation outline:
     * 1. Temporarily treat the separator as removed and run BFS/DFS from any unvisited vertex
     *    to collect its connected component.
     * 2. Repeat the search until every non-separator vertex is assigned to exactly one region.
     * 3. Each region returned should be a standalone set to support independent recursion.
     *
     * @param subgraphVertices vertices from the current recursive block (includes separator)
     * @param separator vertices that must be excluded from the regions
     * @return list of connected vertex sets representing the subgraphs on either side of the separator
     */
    private List<Set<Long>> splitBySeparator(Set<Long> subgraphVertices, Set<Long> separator) {
        // TODO: implement graph traversal that ignores separator vertices and collects connected regions.
        return Collections.emptyList();
    }
}
