package ch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;

import ch.Graph.Edge;

/**
 * left is source and right is target
 */
public class BidirectionalDijkstra {

    public static Result<Integer> shortestPath(Graph g, long s, long t){
        long start = System.nanoTime();
        if (s==t) {
            return new Result<>(System.nanoTime() - start, 0, 0);
        }
        PriorityQueue<PQElem> ql = new PriorityQueue<>(); //forward search pq
        PriorityQueue<PQElem> qr = new PriorityQueue<>(); //backwards search pq
        
        HashSet<Long> settled = new HashSet<>(); //vertices whose distances won't improve
        
        int d = Integer.MAX_VALUE; //shortest distance between s and t
        
        // current best distance estimates in each direction (updated continually)
        HashMap<Long, Integer> dl = new HashMap<>(); //maps a vertex to its tentative distance from left
        HashMap<Long, Integer> dr = new HashMap<>(); //same but for right
        int relaxed = 0;

        
        dl.put(s, 0);
        dr.put(t, 0);
        ql.add(new PQElem(0, s));
        qr.add(new PQElem(0, t));


        while ((!ql.isEmpty()) || (!qr.isEmpty())) {
            PQElem left = ql.peek();
            PQElem right = qr.peek();

            PriorityQueue<PQElem> qi; //reference to the appropriate queue depending on direction
            HashMap<Long, Integer> di; //reference to the appropriate map depending on direction


            //decide which direction to go from
            boolean useLeft;
            if (qr.isEmpty()) {
                useLeft = true;
            } else if (ql.isEmpty()) {
                useLeft = false;
            } else {
                useLeft = ql.peek().key <= qr.peek().key;
            }

            qi = useLeft ? ql : qr;
            di = useLeft ? dl : dr;


            PQElem min = qi.poll();
            long u = min.v;
            int distU = min.key;

            if (settled.contains(u)) {
                break; //if this happens, the two searches have met and we stop
            }
            settled.add(u);
            List<Edge> neighbours = g.getNeighbours(u);

            if (neighbours==null || neighbours.isEmpty()) {
                continue;
            }
            //relaxation step
            for (Graph.Edge e : neighbours){
                relaxed++;
                long v = e.to;
                int weight = e.weight;

                int newDist = distU + weight;
                int oldDist = di.getOrDefault(v, Integer.MAX_VALUE);

                if (newDist < oldDist) {
                    di.put(v, newDist);
                    qi.add(new PQElem(newDist, v));
                }

                if (dl.containsKey(v) && dr.containsKey(v)) {
                    d = Math.min(d, dl.get(v) + dr.get(v));
                }
            }
        }
        if (d==Integer.MAX_VALUE) {
            d=-1;
        }
        long end = System.nanoTime();
        return new Result<>(end - start, relaxed, d);
    }
}
