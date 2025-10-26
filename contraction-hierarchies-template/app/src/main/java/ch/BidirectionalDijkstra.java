package ch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;

public class BidirectionalDijkstra {

    private Graph g;
    private PriorityQueue<PQElem> ql = new PriorityQueue<>();
    private PriorityQueue<PQElem> qr = new PriorityQueue<>();

    private HashSet<Long> settled = new HashSet<>();
    private int d = Integer.MAX_VALUE;

    private HashMap<Long, Integer> dl = new HashMap<>();
    private HashMap<Long, Integer> dr = new HashMap<>();

    public BidirectionalDijkstra(Graph g, long s, long t){
        this.g = g;
        dl.put(s, 0);
        dr.put(t, 0);

        ql.add(new PQElem(0, s));

        qr.add(new PQElem(0, t));
    }
    
    public int search(){
        while ((!ql.isEmpty()) || (!qr.isEmpty())) {
            PQElem left = ql.peek();
            PQElem right = qr.peek();

            PriorityQueue<PQElem> qi; //reference to the appropriate set depending on direction
            HashMap<Long, Integer> di; //reference to the appropriate map depending on direction

            if (!ql.isEmpty() && left.key <= right.key) {
                qi = ql;
                di = dl;
            }else{
                qi = qr;
                di = dr;
            }

            PQElem min = qi.poll();
            long u = min.v;
            int distU = min.key;

            if (settled.contains(u)) {
                break; //if this happens the two serches have met and we stop
            }
            settled.add(u);

            for (Graph.Edge e : g.getNeighbours(u)){
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
        return d;
    }
}