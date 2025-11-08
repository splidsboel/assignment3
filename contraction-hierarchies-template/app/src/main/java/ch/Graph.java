package ch;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
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

    private static final int SHORTCUT_PAIR_CAP = 4096; // heuristic cap on u-v-w combinations per contraction

    private Map<Long, List<Edge>> edges;
    private Map<Long, Vertex> vertices;
    private Map<Long, List<Edge>> incoming; // for every vertex, keep a list of incoming arcs (predecessors)

    public Graph() {
        this.n = 0;
        this.m = 0;
        this.edges = new HashMap<>();
        this.vertices = new HashMap<>();
        this.incoming = new HashMap<>();
    }

    public void addVertex(long id, Vertex v) {
        this.vertices.put(id, v);
        this.n++;
    }

    public void addEdge(long from, long to, long contracted, int weight) {
        Edge edge = new Edge(to, weight, contracted);
        this.edges.computeIfAbsent(from, k -> new ArrayList<>()).add(edge);
        // Mirror the arc in the incoming index so we can fetch predecessors in O(deg⁻(to)).
        this.incoming.computeIfAbsent(to, k -> new ArrayList<>()).add(new Edge(from, weight, contracted));
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
        List<Edge> outgoing = this.edges.get(v);
        List<Edge> outgoingCopy = outgoing == null ? new ArrayList<>() : new ArrayList<>(outgoing);

        // Grab all predecessors but keep edges intact so witness searches see full connectivity.
        List<Edge> predecessors = this.incoming.getOrDefault(v, Collections.emptyList());
        List<Map.Entry<Long, Integer>> incomingEntries = new ArrayList<>(predecessors.size());
        for (Edge inEdge : predecessors) {
            long u = inEdge.to;
            List<Edge> adj = this.edges.get(u);
            if (adj == null) {
                continue;
            }
            for (Edge e : adj) {
                if (e.to == v) {
                    incomingEntries.add(new AbstractMap.SimpleEntry<>(u, e.weight));
                }
            }
        }

        int shortcutsAdded = 0;
        List<Shortcut> newShortcuts = new ArrayList<>();

        if (!incomingEntries.isEmpty() && !outgoingCopy.isEmpty()) {
            if ((long) incomingEntries.size() * (long) outgoingCopy.size() > SHORTCUT_PAIR_CAP) {
                List<Map.Entry<Long, Integer>> trimmedIncoming = new ArrayList<>(incomingEntries);
                trimmedIncoming.sort(Comparator.comparingInt(Map.Entry::getValue));
                int maxIncoming = Math.max(1, SHORTCUT_PAIR_CAP / Math.max(1, outgoingCopy.size()));
                if (trimmedIncoming.size() > maxIncoming) {
                    trimmedIncoming = new ArrayList<>(trimmedIncoming.subList(0, maxIncoming));
                }

                List<Edge> trimmedOutgoing = new ArrayList<>(outgoingCopy);
                trimmedOutgoing.sort(Comparator.comparingInt(e -> e.weight));
                int maxOutgoing = Math.max(1, SHORTCUT_PAIR_CAP / Math.max(1, trimmedIncoming.size()));
                if (trimmedOutgoing.size() > maxOutgoing) {
                    trimmedOutgoing = new ArrayList<>(trimmedOutgoing.subList(0, maxOutgoing));
                }

                incomingEntries = trimmedIncoming;
                outgoingCopy = trimmedOutgoing;
            }

            for (Map.Entry<Long, Integer> inEdge : incomingEntries) {
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

                if (hasWitnessPath(u, w, v, shortcutWeight)) {
                    continue;
                }

                    addEdge(u, w, v, shortcutWeight);
                    shortcutsAdded++;
                    newShortcuts.add(new Shortcut(u, w, shortcutWeight, v));
                }
            }
        }

        // Now remove incoming edges that were previously kept for witness searches.
        int removedIncoming = 0;
        for (Edge inEdge : predecessors) {
            long u = inEdge.to;
            List<Edge> adj = this.edges.get(u);
            if (adj == null) {
                continue;
            }
            Iterator<Edge> it = adj.iterator();
            while (it.hasNext()) {
                Edge e = it.next();
                if (e.to == v) {
                    it.remove();
                    removedIncoming++;
                }
            }
        }
        if (removedIncoming > 0) {
            this.m -= removedIncoming;
        }
        this.incoming.remove(v);

        // Remove outgoing edges from v and purge incoming mirrors.
        if (outgoing != null) {
            this.edges.remove(v);
            int removedOut = outgoing.size();
            for (Edge out : outgoing) {
                List<Edge> inList = this.incoming.get(out.to);
                if (inList == null) {
                    continue;
                }
                Iterator<Edge> it = inList.iterator();
                while (it.hasNext()) {
                    Edge e = it.next();
                    if (e.to == v) {
                        it.remove();
                        break;
                    }
                }
                if (inList.isEmpty()) {
                    this.incoming.remove(out.to);
                }
            }
            this.m -= removedOut;
        }

        if (this.vertices.containsKey(v)) {
            this.vertices.remove(v);
            this.n--;
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
    private boolean hasWitnessPath(long source, long target, long forbidden, int limit) {
        if (limit < 0) {
            return false;
        }
        PriorityQueue<WitnessNode> pq = new PriorityQueue<>();
        Map<Long, Integer> distances = new HashMap<>();
        pq.add(new WitnessNode(source, 0));
        distances.put(source, 0);

        while (!pq.isEmpty()) {
            WitnessNode node = pq.poll();
            if (node.distance > limit) {
                break;
            }
            if (node.vertex == target) {
                return true;
            }
            int best = distances.getOrDefault(node.vertex, Integer.MAX_VALUE);
            if (node.distance > best) {
                continue;
            }

            List<Edge> adj = edges.get(node.vertex);
            if (adj == null) {
                continue;
            }
            for (Edge edge : adj) {
                long next = edge.to;
                if (next == forbidden) {
                    continue;
                }
                int newDist = node.distance + edge.weight;
                if (newDist > limit) {
                    continue;
                }
                int existing = distances.getOrDefault(next, Integer.MAX_VALUE);
                if (newDist < existing) {
                    distances.put(next, newDist);
                    pq.add(new WitnessNode(next, newDist));
                }
            }
        }
        return false;
    }

    private static final class WitnessNode implements Comparable<WitnessNode> {
        final long vertex;
        final int distance;

        WitnessNode(long vertex, int distance) {
            this.vertex = vertex;
            this.distance = distance;
        }

        @Override
        public int compareTo(WitnessNode other) {
            return Integer.compare(this.distance, other.distance);
        }
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
