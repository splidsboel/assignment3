package ch;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class ContractionHierachyTest {

    private Graph buildCycleGraph() {
        Graph g = new Graph();
        g.addVertex(1, new Graph.Vertex(0, 0));
        g.addVertex(2, new Graph.Vertex(1, 0));
        g.addVertex(3, new Graph.Vertex(1, 1));
        g.addVertex(4, new Graph.Vertex(0, 1));

        g.addUndirectedEdge(1, 2, 1);
        g.addUndirectedEdge(2, 3, 1);
        g.addUndirectedEdge(3, 4, 1);
        g.addUndirectedEdge(4, 1, 1);
        return g;
    }

    @Test
    public void testContractionOrderIsPermutation() {
        Graph g = buildCycleGraph();
        ContractionHierachy ch = new ContractionHierachy(g);

        List<Long> order = ch.getContractionOrder();
        assertEquals("Order should include every vertex exactly once", g.getVertexIds().size(), order.size());

        Set<Long> ids = new HashSet<>(g.getVertexIds());
        assertEquals("Order should be a permutation of the vertex set", ids, new HashSet<>(order));

        for (int i = 0; i < order.size(); i++) {
            long vertex = order.get(i);
            assertEquals("Rank should match position in contraction order", Integer.valueOf(i), ch.getRank(vertex));
        }
    }

    @Test
    public void testLazyUpdateUsesFreshEdgeDifference() {
        Graph g = buildCycleGraph();
        ContractionHierachy ch = new ContractionHierachy(g);

        Graph replay = g.copy();
        for (long vertex : ch.getContractionOrder()) {
            Integer recordedDiff = ch.getPriorityAtContraction(vertex);
            assertNotNull("Priority at contraction should be recorded", recordedDiff);

            int currentDiff = replay.getEdgeDifference(vertex);
            assertEquals("Edge difference should be recomputed lazily before contraction", recordedDiff.intValue(), currentDiff);
            replay.contract(vertex);
        }
    }

    @Test
    public void testStoreGraphWritesAugmentedGraph() throws Exception {
        Graph g = buildCycleGraph();
        ContractionHierachy ch = new ContractionHierachy(g);

        Path output = Files.createTempFile("ch-augmented", ".graph");
        try {
            ch.storeGraph(output);
            List<String> lines = Files.readAllLines(output);
            assertFalse("Output should not be empty", lines.isEmpty());

            int n = g.getVertexIds().size();
            String[] header = lines.get(0).trim().split("\\s+");
            assertEquals("Header must contain vertex and edge counts", 2, header.length);
            assertEquals("Vertex count should equal original graph", String.valueOf(n), header[0]);

            int m = Integer.parseInt(header[1]);
            int expectedTotalLines = 1 + n + m;
            assertEquals("File should contain header, vertex lines, and edge lines", expectedTotalLines, lines.size());

            for (int i = 1; i <= n; i++) {
                String[] parts = lines.get(i).trim().split("\\s+");
                assertEquals("Vertex lines should contain id, x, y, rank", 4, parts.length);
                assertNotNull("All vertices should have a recorded rank", parts[3]);
            }

            boolean sawShortcut = false;
            for (int i = 1 + n; i < lines.size(); i++) {
                String[] parts = lines.get(i).trim().split("\\s+");
                assertEquals("Edge lines should contain from, to, weight, contracted", 4, parts.length);
                int contractedId = Integer.parseInt(parts[3]);
                if (contractedId != -1) {
                    sawShortcut = true;
                }
            }
            assertTrue("At least one shortcut edge should be present in the output", sawShortcut);
        } finally {
            Files.deleteIfExists(output);
        }
    }
}
