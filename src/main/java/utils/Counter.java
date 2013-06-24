package utils;

public class Counter {

    private int counter;
    private int final startVal;

    Counter(int startVal) {
        this.startVal = startVal;
        Counter();
    }

    Counter() {
        this.counter = this.startVal;
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