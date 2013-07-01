package utils;

public class Counter {

    private int counter;
    private final int startVal;

    Counter() {
        this(0);
    }

    Counter(int startVal) {
        this.startVal = startVal;
    }

    public int next() {
        this.counter+=1;
        return counter;
    }

    public int reset() {
        this.counter = this.startVal;
        return counter;
    }

}