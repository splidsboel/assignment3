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

        int edgesRead = 0;
        for (int i = 0; i < m; i++) {
            if (!sc.hasNextLong()) {
                throw new IllegalStateException("Expected edge " + i + " 'from' value");
            }
            from = sc.nextLong();
            if (!sc.hasNextLong()) {
                throw new IllegalStateException("Expected edge " + i + " 'to' value");
            }
            to = sc.nextLong();
            if (!sc.hasNextInt()) {
                throw new IllegalStateException("Expected edge " + i + " weight");
            }
            weight = sc.nextInt();
            g.addUndirectedEdge(from, to, weight);
            edgesRead++;
        }

        if (edgesRead != m) {
            throw new IllegalStateException(
                    "Input declared " + m + " edges but only " + edgesRead + " were read");
        }
        System.out.println("Debug flag: M: " + g.m + " N: " + g.n);
        return g;
    }

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        var graph = readGraph(sc);

        // // Nested dissection smoke-test (comment out to skip)
        // long ndStart = System.nanoTime();
        // var ndOrder = graph.computeNestedDissectionOrder();
        // long ndTime = System.nanoTime() - ndStart;
        // System.out.println("Nested dissection vertices: " + ndOrder.size());
        // System.out.println("Nested dissection time (ns): " + ndTime);

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
