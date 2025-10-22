package ch;

public class Result<T> {
    public long time;
    public T result;
    public int relaxed;

    public Result(long time, int relaxed, T result) {
        this.time = time;
        this.relaxed = relaxed;
        this.result = result;
    }
}