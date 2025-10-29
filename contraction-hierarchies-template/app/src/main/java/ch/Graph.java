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
}
