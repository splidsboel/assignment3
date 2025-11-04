package ch;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

class Main {

    private static final String ALG_DIJKSTRA = "dijkstra";
    private static final String ALG_BIDIRECTIONAL = "bidirectional";

    private static Graph readGraph(Scanner sc) {
        int n = sc.nextInt();
        int m = sc.nextInt();

        Graph g = new Graph();

        long id;
        float x, y;

        for (int i = 0; i < n; i++) {
            id = sc.nextLong();
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

    public static void main(String[] args) {
        if (args.length != 1) {
            printUsage();
            return;
        }

        String algorithm = args[0];

        if (!ALG_DIJKSTRA.equals(algorithm) && !ALG_BIDIRECTIONAL.equals(algorithm)) {
            System.err.println("Unknown algorithm: " + algorithm);
            printUsage();
            return;
        }

        Scanner sc = new Scanner(System.in);

        if (!sc.hasNextInt()) {
            System.err.println("Input missing graph size header.");
            sc.close();
            return;
        }

        Graph graph = readGraph(sc);

        if (!sc.hasNextInt()) {
            System.err.println("Input missing query count.");
            sc.close();
            return;
        }

        int queryCount = sc.nextInt();
        List<long[]> queries = new ArrayList<>(queryCount);

        for (int i = 0; i < queryCount; i++) {
            if (!sc.hasNextLong()) {
                System.err.println("Missing source for query " + i);
                break;
            }
            long s = sc.nextLong();
            if (!sc.hasNextLong()) {
                System.err.println("Missing target for query " + i);
                break;
            }
            long t = sc.nextLong();
            queries.add(new long[] { s, t });
        }

        sc.close();

        for (long[] pair : queries) {
            long s = pair[0];
            long t = pair[1];
            Result<Integer> result = runAlgorithm(algorithm, graph, s, t);
            System.out.println(s + "," + t + "," + result.result + "," + result.time + "," + result.relaxed);
        }
    }

    private static Result<Integer> runAlgorithm(String algorithm, Graph graph, long s, long t) {
        if (ALG_DIJKSTRA.equals(algorithm)) {
            return Dijkstra.shortestPath(graph, s, t);
        }
        return BidirectionalDijkstra.shortestPath(graph, s, t);
    }

    private static void printUsage() {
        System.out.println("Usage: java -cp <classpath> ch.Main <algorithm>\n" +
                "  <algorithm>: 'dijkstra' or 'bidirectional'\n" +
                "  stdin must contain the .graph contents followed by:\n" +
                "    <query_count>\n" +
                "    <s0> <t0>\n" +
                "    ...\n" +
                "    <sq-1> <tq-1>");
    }
}
