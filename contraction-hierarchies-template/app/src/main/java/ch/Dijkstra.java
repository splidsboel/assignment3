package ch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class Dijkstra {
    /**
     * Computes the shortest path distance between two vertices using Dijkstra's algorithm.
     * Returns -1 if there is no path.
     * @param g    The graph to search.
     * @param from The starting vertex ID.
     * @param to   The target vertex ID.
     * @return A triple containing the duration (in ns), the number of relaxed edges, and the shortest path distance. The distance is -1 if no path exists.
     */
    public static Result<Integer> shortestPath(Graph g, long from, long to) {
        long start = System.nanoTime();
        PriorityQueue<PQElem> pq = new PriorityQueue<>();
        Set<Long> visited = new HashSet<>();
        Map<Long, Integer> dists = new HashMap<>();
        int relaxed = 0;

        pq.add(new PQElem(0, from));
        dists.put(from, 0);
        
        while (!pq.isEmpty()&& pq.peek().v != to) {
            PQElem elem = pq.poll();
            long u = elem.v;
            if (visited.contains(u)) {
                continue;
            }

            visited.add(u);

            int dist = elem.key;
            for (Graph.Edge e : g.getNeighbours(u)) {
                relaxed++;
                long v = e.to;
                int w = e.weight;
                if (!dists.containsKey(v) || dists.get(v) > dist + w) {
                    pq.add(new PQElem(dist + w, v));
                    dists.put(v, dist + w);
                }
            }
        }
        long end = System.nanoTime();
        if (!dists.containsKey(to)) {
            return new Result<>(end - start, relaxed, -1);
        }
        return new Result<>(end - start, relaxed, dists.get(to));
    }
}