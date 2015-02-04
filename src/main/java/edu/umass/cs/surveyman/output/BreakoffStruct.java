package edu.umass.cs.surveyman.output;

import java.util.HashMap;

abstract class BreakoffStruct<K> extends HashMap<K, Integer>{

    class Pair implements Comparable {

        final K thing;
        final int frequency;

        public Pair(K thing, int frequency) {
            this.frequency = frequency;
            this.thing = thing;
        }

        @Override
        public int compareTo(Object o) {
            Pair that = (Pair) o;
            if (this.frequency > that.frequency)
                return -1;
            else if (this.frequency < that.frequency)
                return 1;
            else return 0;
        }
    }

    private K k;

    abstract public void update(K k);

    abstract public String jsonize();

    @Override
    abstract public String toString();

}
