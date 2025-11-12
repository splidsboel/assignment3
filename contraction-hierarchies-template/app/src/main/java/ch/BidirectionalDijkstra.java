package ch;

import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

import ch.Graph.Edge;

/**
 * Bidirectional Dijkstra for Contraction Hierarchies
 * Forward expands outgoing arcs (upward in rank).
 * Backward expands incoming arcs (also upward in rank, since it's on the reverse graph).
 */
public class BidirectionalDijkstra {

    // --- Debug knobs ---
    // Set to true to print a running trace. Keep small limits or it gets very chatty.
    private static final boolean DEBUG = false;
    // How many node expansions to print per side (forward+backward combined)
    private static final int DEBUG_EXPANSION_LIMIT = 10;  // set 0 to disable prints
    // How many skipped-neighbor reasons to print
    private static final int DEBUG_SKIP_LIMIT = 10;

    public static Result<Integer> shortestPath(Graph g, long s, long t) {
        return shortestPath(g, s, t, null);
    }

    public static Result<Integer> shortestPath(Graph g, long s, long t, java.util.Map<Long, Integer> ranks) {
        long start = System.nanoTime();
        if (s == t) {
            return new Result<>(System.nanoTime() - start, 0, 0);
        }

        PriorityQueue<PQElem> ql = new PriorityQueue<>(); // forward
        PriorityQueue<PQElem> qr = new PriorityQueue<>(); // backward

        HashMap<Long, Integer> dl = new HashMap<>(); // forward distances
        HashMap<Long, Integer> dr = new HashMap<>(); // backward distances

        int best = Integer.MAX_VALUE;
        int relaxed = 0;

        dl.put(s, 0); ql.add(new PQElem(0, s));
        dr.put(t, 0); qr.add(new PQElem(0, t));

        // Debug counters
        int dbgExpandPrinted = 0;
        int dbgSkipPrinted = 0;

        while (!ql.isEmpty() || !qr.isEmpty()) {
            int minForward  = ql.isEmpty() ? Integer.MAX_VALUE : ql.peek().key;
            int minBackward = qr.isEmpty() ? Integer.MAX_VALUE : qr.peek().key;

            if (Math.min(minForward, minBackward) >= best) {
                if (DEBUG && DEBUG_EXPANSION_LIMIT > 0) {
                    System.out.printf("STOP: minQueueKey=%d best=%d%n", Math.min(minForward, minBackward), best);
                }
                break;
            }

            boolean forward = minForward <= minBackward;
            PriorityQueue<PQElem> pq = forward ? ql : qr;
            HashMap<Long, Integer> dist = forward ? dl : dr;

            PQElem cur = pq.poll();
            long u = cur.v;
            int du = cur.key;

            int recorded = dist.getOrDefault(u, Integer.MAX_VALUE);
            if (du > recorded) continue;

            // Meet update if other side has reached u
            if (forward && dr.containsKey(u)) best = Math.min(best, du + dr.get(u));
            if (!forward && dl.containsKey(u)) best = Math.min(best, du + dl.get(u));

            // ---- STALL ON DEMAND ----
boolean stall;
if (forward) {
    stall = shouldStallForward(g, u, du, dl, dr, ranks);
} else {
    stall = shouldStallBackward(g, u, du, dl, dr, ranks);
}

if (stall) {
    if (DEBUG) {
        System.out.printf("[%s] STALL u=%d du=%d%n", forward ? "FWD" : "BWD", u, du);
    }
    continue; // skip relaxing neighbours
}

            // Use outgoing for forward, incoming for backward
            List<Edge> neighbours = forward ? g.getNeighbours(u) : g.getIncoming(u);
            if (neighbours == null || neighbours.isEmpty()) {
                if (DEBUG && DEBUG_EXPANSION_LIMIT > 0 && dbgExpandPrinted < DEBUG_EXPANSION_LIMIT) {
                    System.out.printf("[%s] u=%d du=%d rankU=%s deg(out)=%d deg(in)=%d  -> no neighbours%n",
                            forward ? "FWD" : "BWD",
                            u, du,
                            (ranks == null ? "null" : String.valueOf(ranks.get(u))),
                            g.getNeighbours(u) == null ? 0 : g.getNeighbours(u).size(),
                            g.getIncoming(u) == null ? 0 : g.getIncoming(u).size());
                    dbgExpandPrinted++;
                }
                continue;
            }

            Integer rankU = (ranks == null) ? null : ranks.get(u);

            if (DEBUG && DEBUG_EXPANSION_LIMIT > 0 && dbgExpandPrinted < DEBUG_EXPANSION_LIMIT) {
                System.out.printf("[%s] u=%d du=%d rankU=%s deg(out)=%d deg(in)=%d%n",
                        forward ? "FWD" : "BWD",
                        u, du,
                        (ranks == null ? "null" : String.valueOf(rankU)),
                        g.getNeighbours(u) == null ? 0 : g.getNeighbours(u).size(),
                        g.getIncoming(u) == null ? 0 : g.getIncoming(u).size());
                dbgExpandPrinted++;
            }

            for (Edge e : neighbours) {
                long v = e.to; // NOTE: on backward step, incoming list stores (v -> u), so e.to is predecessor
                int newDist = du + e.weight;

                if (newDist >= best) {
                    if (DEBUG && DEBUG_SKIP_LIMIT > 0 && dbgSkipPrinted < DEBUG_SKIP_LIMIT) {
                        System.out.printf("  skip@bound u=%d -> v=%d newDist=%d >= best=%d%n",
                                u, v, newDist, best);
                        dbgSkipPrinted++;
                    }
                    continue;
                }

                // Rank gating: apply ONLY if both ranks are known
                if (ranks != null) {
                    Integer rankV = ranks.get(v);
                    if (rankU != null && rankV != null) {
                        // Upward in BOTH directions (forward on G, backward on G^R)
                        if (rankV <= rankU) {
                            if (DEBUG && DEBUG_SKIP_LIMIT > 0 && dbgSkipPrinted < DEBUG_SKIP_LIMIT) {
                                System.out.printf("  skip@rank u=%d(r=%d) -> v=%d(r=%d) dir=%s need rankV>rankU%n",
                                        u, rankU, v, rankV, forward ? "up-FWD" : "up-BWD");
                                dbgSkipPrinted++;
                            }
                            continue;
                        }
                    }
                    // If one of the ranks is missing, do not gate on rank.
                }

                relaxed++;
                int old = dist.getOrDefault(v, Integer.MAX_VALUE);
                if (newDist < old) {
                    dist.put(v, newDist);
                    pq.add(new PQElem(newDist, v));

                    // Meet update via v
                    if (forward && dr.containsKey(v)) best = Math.min(best, newDist + dr.get(v));
                    if (!forward && dl.containsKey(v)) best = Math.min(best, newDist + dl.get(v));
                }
            }
        }

        if (best == Integer.MAX_VALUE) best = -1;
        long end = System.nanoTime();

        if (DEBUG) {
            System.out.printf("distance=%d relaxed=%d time(ns)=%d%n", best, relaxed, (end - start));
        }
        return new Result<>(end - start, relaxed, best);
    }
    private static boolean shouldStallForward(Graph g,
                                          long u,
                                          int du,
                                          HashMap<Long,Integer> dl,
                                          HashMap<Long,Integer> dr,
                                          java.util.Map<Long,Integer> ranks) {

    // Stall test: look for a neighbor v with rank(v) > rank(u) and
    // dl(v) + w(v→u) < dl(u).   That means better path exists via v.
    var incoming = g.getIncoming(u);
    if (incoming == null) return false;

    Integer rankU = (ranks == null) ? null : ranks.get(u);

    for (Graph.Edge e : incoming) {
        long v = e.to;
        Integer dv = dl.get(v);
        if (dv == null) continue;

        if (ranks != null && rankU != null) {
            Integer rankV = ranks.get(v);
            if (rankV != null && rankV <= rankU) continue; // upward only
        }

        if (dv + e.weight < du) return true;
    }
    return false;
}

private static boolean shouldStallBackward(Graph g,
                                           long u,
                                           int du,
                                           HashMap<Long,Integer> dl,
                                           HashMap<Long,Integer> dr,
                                           java.util.Map<Long,Integer> ranks) {

    // Stall test on reverse graph: check outgoing edges of u
    var outgoing = g.getNeighbours(u);
    if (outgoing == null) return false;

    Integer rankU = (ranks == null) ? null : ranks.get(u);

    for (Graph.Edge e : outgoing) {
        long v = e.to;
        Integer dv = dr.get(v);
        if (dv == null) continue;

        if (ranks != null && rankU != null) {
            Integer rankV = ranks.get(v);
            if (rankV != null && rankV <= rankU) continue; // upward only
        }

        if (dv + e.weight < du) return true;
    }
    return false;
}

}
