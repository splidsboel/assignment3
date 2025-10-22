package ch;

public class PQElem implements Comparable<PQElem> {
    int key;
    long v;

    public PQElem(int key, long v) {
        this.key = key;
        this.v = v;
    }

    @Override
    public int compareTo(PQElem o) {
        return key - o.key;
    }
}