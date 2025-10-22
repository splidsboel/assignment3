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
        sc.close();
        System.out.println(graph.n + " " + graph.m);
    }
}