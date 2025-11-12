package ch;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Locale;

public class ContractionHierachy {

    private final Graph originalGraph;
    private final Graph workingGraph;
    private final List<Long> contractionOrder;
    private final Map<Long, Integer> rank;
    private final Map<Long, Integer> shortcutsPerVertex;
    private final Map<Long, Integer> priorityAtContraction;
    private final Map<ShortcutKey, Graph.Shortcut> shortcuts;

    private static final class QueueEntry implements Comparable<QueueEntry> {
        final long vertex;
        final int priority;

        QueueEntry(long vertex, int priority) {
            this.vertex = vertex;
            this.priority = priority;
        }

        @Override
        public int compareTo(QueueEntry other) {
            int cmp = Integer.compare(this.priority, other.priority);
            if (cmp != 0) {
                return cmp;
            }
            return Long.compare(this.vertex, other.vertex);
        }
    }

    public ContractionHierachy(Graph graph) {
        if (graph == null) {
            throw new IllegalArgumentException("Graph must not be null");
        }
        this.originalGraph = graph;
        this.workingGraph = graph.copy();
        System.out.println("Debug flag: Size of workingGraph:" + workingGraph.getVertexIds().size());
        this.contractionOrder = new ArrayList<>();
        this.rank = new HashMap<>();
        this.shortcutsPerVertex = new HashMap<>();
        this.priorityAtContraction = new HashMap<>();
        this.shortcuts = new HashMap<>();
        preprocess();
    }


private void preprocess() {
    // Build a snapshot of vertices from the working graph
    Set<Long> vertices = workingGraph.getVertexIds();
    final int totalVertices = vertices.size();
    if (totalVertices == 0) {
        System.out.println("CH preprocessing: no vertices to contract.");
        return;
    }

    System.out.printf("CH preprocessing: contracting %,d vertices...%n", totalVertices);

    // 1) Initial PQ of (vertex, edge-difference)
    PriorityQueue<QueueEntry> pq = new PriorityQueue<>();
    for (long v : vertices) {
        pq.add(new QueueEntry(v, workingGraph.getEdgeDifference(v)));
    }

    // Rank counter (0..n-1). Using an explicit counter avoids off-by-ones.
    int nextRank = 0;
    int processed = 0;

    // Optional: progress logging
    final int progressStep = Math.max(1, totalVertices / 10);
    int nextCheckpoint = progressStep;

    // 2) Main loop with LAZY UPDATE: re-evaluate the top before contraction
    while (!pq.isEmpty()) {
        QueueEntry top = pq.poll();
        long v = top.vertex;

        // Vertex might already be contracted (removed) -> skip
        if (!workingGraph.containsVertex(v)) {
            continue;
        }

        // Lazy re-evaluation of edge difference BEFORE contracting
        int freshDiff = workingGraph.getEdgeDifference(v);
        if (freshDiff != top.priority) {
            // Priority became stale -> reinsert with the new priority, skip contracting this round
            pq.add(new QueueEntry(v, freshDiff));
            continue;
        }

        // 3) Contract v now that we confirmed its current priority
        Graph.ContractResult cr = workingGraph.contract(v);

        // 4) Assign rank immediately and uniquely
        rank.put(v, nextRank++);

        // Bookkeeping (optional; you already have these maps):
        contractionOrder.add(v);
        shortcutsPerVertex.put(v, cr.shortcutsAdded);
        priorityAtContraction.put(v, freshDiff);
        recordShortcuts(cr.shortcuts);

        processed++;
        if (processed >= nextCheckpoint || processed == totalVertices) {
            double percent = (processed * 100.0) / totalVertices;
            System.out.printf("  %,d/%,d (%.1f%%) contracted%n", processed, totalVertices, percent);
            nextCheckpoint += progressStep;
        }
    }

    // 5) Strong postconditions: every vertex must have a rank in [0..n-1]
    if (nextRank != totalVertices) {
        throw new IllegalStateException(
            "Preprocess finished but not all vertices were ranked: nextRank=" + nextRank +
            " totalVertices=" + totalVertices);
    }
    if (rank.size() != totalVertices) {
        throw new IllegalStateException(
            "Rank map size mismatch: " + rank.size() + " vs total=" + totalVertices);
    }
}


    public List<Long> getContractionOrder() {
        return Collections.unmodifiableList(contractionOrder);
    }

    public Integer getRank(long vertex) {
        return rank.get(vertex);
    }

    public Integer getShortcutsFor(long vertex) {
        return shortcutsPerVertex.get(vertex);
    }

    public Integer getPriorityAtContraction(long vertex) {
        return priorityAtContraction.get(vertex);
    }

    int getShortcutCountForTesting() {
        return shortcuts.size();
    }

    public Result<Integer> query(long s, long t) {
        return BidirectionalDijkstra.shortestPath(originalGraph, s, t);
    }

    public void storeGraph(Path outputPath) throws IOException {
        if (outputPath == null) {
            throw new IllegalArgumentException("Output path must not be null");
        }

        List<Long> vertexIds = new ArrayList<>(originalGraph.getVertexIds());
        Collections.sort(vertexIds);

        List<EdgeRecord> edges = new ArrayList<>();
        for (long from : vertexIds) {
            List<Graph.Edge> neighbours = originalGraph.getNeighbours(from);
            if (neighbours == null) {
                continue;
            }
            for (Graph.Edge edge : neighbours) {
                edges.add(new EdgeRecord(from, edge.to, edge.weight, edge.contracted));
            }
        }

        for (Graph.Shortcut shortcut : shortcuts.values()) {
            edges.add(new EdgeRecord(shortcut.from, shortcut.to, shortcut.weight, shortcut.via));
        }

        edges.sort(Comparator
                .comparingLong((EdgeRecord e) -> e.from)
                .thenComparingLong(e -> e.to)
                .thenComparingInt(e -> e.weight)
                .thenComparingLong(e -> e.via));

        int edgeCount = edges.size();
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            writer.write(String.format(Locale.US, "%d %d%n", vertexIds.size(), edgeCount));
            for (long id : vertexIds) {
                Graph.Vertex vertex = originalGraph.getVertex(id);
                int vertexRank = rank.getOrDefault(id, -1);
                writer.write(String.format(Locale.US, "%d %f %f %d%n", id, vertex.x, vertex.y, vertexRank));
            }
            for (EdgeRecord edge : edges) {
                writer.write(String.format(Locale.US, "%d %d %d %d%n", edge.from, edge.to, edge.weight, edge.via));
            }
        }
    }

    private void recordShortcuts(List<Graph.Shortcut> newShortcuts) {
        for (Graph.Shortcut shortcut : newShortcuts) {
            ShortcutKey key = new ShortcutKey(shortcut.from, shortcut.to);
            shortcuts.merge(key, shortcut,
                    (existing, candidate) -> existing.weight <= candidate.weight ? existing : candidate);
        }
    }

    private static final class EdgeRecord {
        final long from;
        final long to;
        final int weight;
        final long via;

        EdgeRecord(long from, long to, int weight, long via) {
            this.from = from;
            this.to = to;
            this.weight = weight;
            this.via = via;
        }
    }

    private static final class ShortcutKey {
        final long from;
        final long to;

        ShortcutKey(long from, long to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ShortcutKey)) {
                return false;
            }
            ShortcutKey other = (ShortcutKey) obj;
            return this.from == other.from && this.to == other.to;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(from) * 31 + Long.hashCode(to);
        }
    }
}
