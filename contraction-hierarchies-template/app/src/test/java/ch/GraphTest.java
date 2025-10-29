package ch;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.junit.Test;

public class GraphTest {

    @Test
    public void testContractAddsShortcutsAndRemovesVertex() {
        Graph g = new Graph();
        g.addVertex(1, new Graph.Vertex(0, 0));
        g.addVertex(2, new Graph.Vertex(1, 1));
        g.addVertex(3, new Graph.Vertex(2, 2));

        g.addUndirectedEdge(1, 2, 1);
        g.addUndirectedEdge(2, 3, 2);

        Graph.ContractResult result = g.contract(2);

        assertEquals("Contracting vertex should add two directed shortcuts", 2, result.shortcutsAdded);
        assertEquals("Shortcut list should contain the directed edges that were added", 2, result.shortcuts.size());
        assertEquals("Vertex count should decrease after contraction", 2, g.n);
        assertEquals("Edge count should reflect only the new shortcuts", 2, g.m);
        assertNull("Contracted vertex should be removed from vertex map", g.getVertex(2));
        assertNull("Neighbors for contracted vertex should be cleared", g.getNeighbours(2));

        List<Graph.Edge> fromOne = g.getNeighbours(1);
        assertNotNull("Vertex 1 should keep outgoing edges after contraction", fromOne);
        assertEquals("Only the shortcut edge should remain from vertex 1", 1, fromOne.size());
        Graph.Edge shortcut = fromOne.get(0);
        assertEquals("Shortcut should point to vertex 3", 3, shortcut.to);
        assertEquals("Shortcut weight should equal sum of incident edges", 3, shortcut.weight);
        assertEquals("Shortcut should record the contracted vertex id", 2, shortcut.contracted);

        List<Graph.Edge> fromThree = g.getNeighbours(3);
        assertNotNull("Vertex 3 should receive the reverse shortcut", fromThree);
        assertEquals("Only the reverse shortcut should remain from vertex 3", 1, fromThree.size());
        Graph.Edge reverse = fromThree.get(0);
        assertEquals("Reverse shortcut should point back to vertex 1", 1, reverse.to);
        assertEquals("Reverse shortcut should carry the symmetric weight", 3, reverse.weight);
        assertEquals("Reverse shortcut should record the contracted vertex id", 2, reverse.contracted);
    }

    @Test
    public void testGetEdgeDifferenceReflectsShortcutNeeds() {
        Graph g = new Graph();
        g.addVertex(1, new Graph.Vertex(0, 0));
        g.addVertex(2, new Graph.Vertex(1, 1));
        g.addVertex(3, new Graph.Vertex(2, 2));
        g.addVertex(4, new Graph.Vertex(3, 3));

        g.addUndirectedEdge(2, 1, 2);
        g.addUndirectedEdge(2, 3, 3);
        g.addUndirectedEdge(2, 4, 4);

        int edgeDiffWithoutExistingShortcuts = g.getEdgeDifference(2);
        assertEquals("Balanced neighborhood should result in equal shortcuts and removals", 0, edgeDiffWithoutExistingShortcuts);

        g.addUndirectedEdge(1, 3, 1);
        int edgeDiffWithExistingBetterEdge = g.getEdgeDifference(2);
        assertEquals("Existing better edges should reduce the edge difference", -1, edgeDiffWithExistingBetterEdge);
    }

    @Test
    public void testContractPreservesShortestPathsForTestGraph() throws Exception {
        GraphFixture fixture = loadGraph(resolveTestGraph());
        Graph g = fixture.graph;
        long[] vertexIds = fixture.vertexIds;
        long contractedVertex = 6L;

        Map<String, Integer> baseline = new HashMap<>();
        for (long from : vertexIds) {
            if (from == contractedVertex) {
                continue;
            }
            for (long to : vertexIds) {
                if (to == contractedVertex) {
                    continue;
                }
                Result<Integer> result = BidirectionalDijkstra.shortestPath(g, from, to);
                baseline.put(key(from, to), result.result);
            }
        }

        g.contract(contractedVertex);

        for (long from : vertexIds) {
            if (from == contractedVertex) {
                continue;
            }
            for (long to : vertexIds) {
                if (to == contractedVertex) {
                    continue;
                }
                Result<Integer> result = BidirectionalDijkstra.shortestPath(g, from, to);
                String k = key(from, to);
                Integer expected = baseline.get(k);
                assertNotNull("Baseline distance missing for " + k, expected);
                assertEquals("Contracting vertex " + contractedVertex + " should preserve distance for " + k,
                        expected.longValue(), (long) result.result);
            }
        }
    }

    private static Path resolveTestGraph() {
        Path candidate = Paths.get("test.graph");
        if (Files.exists(candidate)) {
            return candidate;
        }
        candidate = Paths.get("..", "test.graph");
        if (Files.exists(candidate)) {
            return candidate;
        }
        throw new IllegalStateException("Unable to locate test.graph file");
    }

    private static GraphFixture loadGraph(Path path) throws IOException {
        try (Scanner sc = new Scanner(Files.newInputStream(path))) {
            int n = sc.nextInt();
            int m = sc.nextInt();

            Graph g = new Graph();
            long[] vertexIds = new long[n];

            for (int i = 0; i < n; i++) {
                long id = sc.nextLong();
                vertexIds[i] = id;
                float x = sc.nextFloat();
                float y = sc.nextFloat();
                g.addVertex(id, new Graph.Vertex(x, y));
            }

            for (int i = 0; i < m; i++) {
                long from = sc.nextLong();
                long to = sc.nextLong();
                int weight = sc.nextInt();
                g.addUndirectedEdge(from, to, weight);
            }

            return new GraphFixture(g, vertexIds);
        }
    }

    private static String key(long from, long to) {
        return from + "->" + to;
    }

    private static class GraphFixture {
        final Graph graph;
        final long[] vertexIds;

        GraphFixture(Graph graph, long[] vertexIds) {
            this.graph = graph;
            this.vertexIds = vertexIds;
        }
    }
}
