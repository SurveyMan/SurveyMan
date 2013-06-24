package utils;

class Gensym {

    private static int counter = 0;
    private final String prefix;

    Gensym (String prefix) {
        this.prefix = prefix;
    }

    Gensym () {
        this.prefix = "";
    }

    public String gensym() {
        counter += 1;
        return prefix + counter;
    }
}