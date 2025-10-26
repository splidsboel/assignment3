package ch;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class BidirectionalDijkstraTest {
    private Graph g;
    
    @Before
    public void setUp() {
        g = new Graph();
        g.addVertex(1, new   Graph.Vertex(0, 0));
        g.addVertex(2, new Graph.Vertex(1, 1));
        g.addVertex(3, new Graph.Vertex(2, 2));
        g.addVertex(4, new Graph.Vertex(3, 3));

        // Graph structure:
        // 1 --(4)-- 2 --(1)-- 3
        //  \                /
        //   (8)          (2)
        //     \          /
        //          4
        g.addUndirectedEdge(1, 2, 4);
        g.addUndirectedEdge(2, 3, 1);
        g.addUndirectedEdge(1, 4, 8);
        g.addUndirectedEdge(3, 4, 2);
    }

        @Test
    public void testShortestPath_basic() {
        int dist = BidirectionalDijkstra.shortestPath(g, 1, 4);
        assertEquals("Shortest path 1→2→3→4 should have total cost 7", (long) dist, 7);
    }

    @Test
    public void testShortestPath_directConnection() {
        int dist = BidirectionalDijkstra.shortestPath(g, 1, 2);
        assertEquals("Direct edge 1→2 should cost 4", (long) dist, 4 );
    }

    @Test
    public void testShortestPath_noPathExists() {
        Graph disconnected = new Graph();
        disconnected.addVertex(1, new Graph.Vertex(0, 0));
        disconnected.addVertex(2, new Graph.Vertex(1, 1));
        disconnected.addVertex(3, new Graph.Vertex(2, 2));
        disconnected.addUndirectedEdge(1, 2, 4);
        // no edges between 1 and 3
        int dist = BidirectionalDijkstra.shortestPath(disconnected, 1, 3);
        assertEquals("Unreachable vertex should return -1",-1, (long) dist );
    }
    @Test
    public void testShortestPath_sameNode() {
        int dist = BidirectionalDijkstra.shortestPath(g, 1, 1);
        assertEquals("Distance from a node to itself should be 0", 0, (long) dist);
}
}
