package ch;

import java.util.Scanner;

class Main {

    private static Graph readGraph(Scanner sc) {
        int n = sc.nextInt();
        int m = sc.nextInt();

        Graph g = new Graph();

        long id;
        float x, y;
        long[] ids = new long[n];

        for (int i = 0; i < n; i++) {
            id = sc.nextLong();

            ids[i] = id;
            x = Float.parseFloat(sc.next());
            y = Float.parseFloat(sc.next());

            g.addVertex(id, new Graph.Vertex(x, y));
        }

        long from, to;
        int weight;

        for (int i = 0; i < m; i++) {
            from = sc.nextLong();
            to = sc.nextLong();
            weight = sc.nextInt();
            g.addUndirectedEdge(from, to, weight);
        }

        return g;
    }

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        var graph = readGraph(sc);
        ContractionHierachy ch = new ContractionHierachy(graph);
        ch.storeGraph(java.nio.file.Path.of("denmark-augmented.graph"));
        sc.close();
        System.out.println(graph.n + " " + graph.m);
        Result<Integer> result = BidirectionalDijkstra.shortestPath(graph, 4, 5);
        // System.out.println("Shortest path from 4 to 5 in test.graph is: " + result.result);
        // System.out.println("Relaxed edges: " + result.relaxed);
        System.out.println("Computation time (ns): " + result.time);
    }
}
