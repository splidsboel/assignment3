package ch;

import org.junit.Before;

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
}
