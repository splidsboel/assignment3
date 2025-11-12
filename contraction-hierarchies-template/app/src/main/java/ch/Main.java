package ch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

class Main {

    public static void main(String[] args) throws Exception {
        if (args.length == 2 && "preprocess".equalsIgnoreCase(args[0])) {
            runPreprocess(Path.of(args[1]));
        } else if (args.length == 4 && "query".equalsIgnoreCase(args[0])) {
            runQuery(Path.of(args[1]), Long.parseLong(args[2]), Long.parseLong(args[3]));
        } else if (args.length == 4 && "query-raw".equalsIgnoreCase(args[0])) {
            runRawQuery(Path.of(args[1]), Long.parseLong(args[2]), Long.parseLong(args[3]));
        } else if (args.length == 4 && "query-dijkstra".equalsIgnoreCase(args[0])) {
            runDijkstraQuery(Path.of(args[1]), Long.parseLong(args[2]), Long.parseLong(args[3]));
        } else {
            printUsage();
        }
    }
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
    
    private static void runPreprocess(Path output) throws IOException {
        try (Scanner sc = new Scanner(System.in)) {
            Graph graph = readOriginalGraph(sc);
            ContractionHierachy ch = new ContractionHierachy(graph);
            ch.storeGraph(output);
            System.out.printf("Stored augmented graph at %s%n", output);
        }
    }

    private static void runQuery(Path augmented, long source, long target) throws IOException {
        LoadedGraph loaded = readAugmentedGraph(augmented);
        Result<Integer> result = BidirectionalDijkstra.shortestPath(loaded.graph, source, target, loaded.ranks);
        System.out.printf("distance=%d relaxed=%d time(ns)=%d%n", result.result, result.relaxed, result.time);
    }
    
    private static void runRawQuery(Path originalGraph, long source, long target) throws IOException {
        Graph graph = readOriginalGraph(originalGraph);
        Result<Integer> result = BidirectionalDijkstra.shortestPath(graph, source, target);
        System.out.printf("distance=%d relaxed=%d time(ns)=%d%n", result.result, result.relaxed, result.time);
    }

    private static void runDijkstraQuery(Path originalGraph, long source, long target) throws IOException {
        Graph graph = readOriginalGraph(originalGraph);
        Result<Integer> result = Dijkstra.shortestPath(graph, source, target);
        System.out.printf("distance=%d relaxed=%d time(ns)=%d%n", result.result, result.relaxed, result.time);
    }

    private static Graph readOriginalGraph(Path path) throws IOException {
        try (Scanner sc = new Scanner(Files.newBufferedReader(path))) {
            return readOriginalGraph(sc);
        }
    }
 
 
    private static Graph readOriginalGraph(Scanner sc) {
        int n = sc.nextInt();
        int m = sc.nextInt();
        Graph g = new Graph();
        for (int i = 0; i < n; i++) {
            long id = sc.nextLong();
            float x = Float.parseFloat(sc.next());
            float y = Float.parseFloat(sc.next());
            g.addVertex(id, new Graph.Vertex(x, y));
        }
        int edgesRead = 0;
        for (int i = 0; i < m; i++) {
            if (!sc.hasNextLong()) {
                throw new IllegalStateException("Expected edge " + i + " 'from' value");
            }
            long from = sc.nextLong();
            if (!sc.hasNextLong()) {
                throw new IllegalStateException("Expected edge " + i + " 'to' value");
            }
            long to = sc.nextLong();
            if (!sc.hasNextInt()) {
                throw new IllegalStateException("Expected edge " + i + " weight");
            }
            int weight = sc.nextInt();
            g.addUndirectedEdge(from, to, weight);
            edgesRead++;
        }
        if (edgesRead != m) {
            throw new IllegalStateException(
                    "Input declared " + m + " edges but only " + edgesRead + " were read");
        }
        return g;
    }

    private static LoadedGraph readAugmentedGraph(Path path) throws IOException {
        try (Scanner sc = new Scanner(Files.newBufferedReader(path))) {
            int n = sc.nextInt();
            int m = sc.nextInt();
            Graph g = new Graph();
            Map<Long, Integer> ranks = new HashMap<>();

            for (int i = 0; i < n; i++) {
                long id = sc.nextLong();
                float x = Float.parseFloat(sc.next());
                float y = Float.parseFloat(sc.next());
                int rank = sc.nextInt();
                g.addVertex(id, new Graph.Vertex(x, y));
                ranks.put(id, rank);
            }

            for (int i = 0; i < m; i++) {
                long from = sc.nextLong();
                long to = sc.nextLong();
                int weight = sc.nextInt();
                long via = sc.nextLong();
                g.addEdge(from, to, via, weight);
            }
            return new LoadedGraph(g, ranks);
        }
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  preprocess <output.graph>   # reads original graph from stdin");
        System.out.println("  query <augmented.graph> <source> <target>");
        System.out.println("  query-raw <graph> <source> <target>   # run queries on unprocessed graph files");
        System.out.println("  query-dijkstra <graph> <source> <target>   # run plain Dijkstra on unprocessed graphs");
    }

    private static final class LoadedGraph {
        final Graph graph;
        final Map<Long, Integer> ranks;

        LoadedGraph(Graph graph, Map<Long, Integer> ranks) {
            this.graph = graph;
            this.ranks = ranks;
        }
    }
}
