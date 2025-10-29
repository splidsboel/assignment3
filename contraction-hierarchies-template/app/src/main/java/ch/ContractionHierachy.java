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
    private final List<Graph.Shortcut> shortcuts;

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
        this.contractionOrder = new ArrayList<>();
        this.rank = new HashMap<>();
        this.shortcutsPerVertex = new HashMap<>();
        this.priorityAtContraction = new HashMap<>();
        this.shortcuts = new ArrayList<>();
        preprocess();
    }

    private void preprocess() {
        PriorityQueue<QueueEntry> pq = new PriorityQueue<>();
        Set<Long> vertices = workingGraph.getVertexIds();
        int totalVertices = vertices.size();
        if (totalVertices == 0) {
            System.out.println("CH preprocessing: no vertices to contract.");
            return;
        }
        int progressStep = Math.max(1, (int) Math.ceil(totalVertices / 1000.0)); // log roughly every 0.1%
        int nextCheckpoint = progressStep;
        System.out.printf("CH preprocessing: contracting %,d vertices...%n", totalVertices);

        for (long v : vertices) {
            int diff = workingGraph.getEdgeDifference(v);
            pq.add(new QueueEntry(v, diff));
        }

        int processed = 0;
        while (!pq.isEmpty()) {
            QueueEntry entry = pq.poll();
            long vertex = entry.vertex;

            if (!workingGraph.containsVertex(vertex)) {
                continue;
            }

            int currentDiff = workingGraph.getEdgeDifference(vertex);
            if (currentDiff != entry.priority) {
                pq.add(new QueueEntry(vertex, currentDiff));
                continue;
            }

            Graph.ContractResult contractResult = workingGraph.contract(vertex);
            contractionOrder.add(vertex);
            rank.put(vertex, contractionOrder.size() - 1);
            shortcutsPerVertex.put(vertex, contractResult.shortcutsAdded);
            priorityAtContraction.put(vertex, currentDiff);
            shortcuts.addAll(contractResult.shortcuts);

            processed++;
            if (processed >= nextCheckpoint || processed == totalVertices) {
                double percent = processed * 100.0 / totalVertices;
                System.out.printf("CH preprocessing: %,d/%,d vertices processed (%.1f%%)%n",
                        processed, totalVertices, percent);
                while (nextCheckpoint <= processed) {
                    nextCheckpoint += progressStep;
                }
            }
        }
        System.out.println("CH preprocessing: completed.");
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

        for (Graph.Shortcut shortcut : shortcuts) {
            edges.add(new EdgeRecord(shortcut.from, shortcut.to, shortcut.weight, shortcut.via));
        }

        edges.sort(Comparator
                .comparingLong((EdgeRecord e) -> e.from)
                .thenComparingLong(e -> e.to)
                .thenComparingInt(e -> e.weight)
                .thenComparingLong(e -> e.via));

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            writer.write(String.format(Locale.US, "%d %d%n", vertexIds.size(), edges.size()));
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
}
