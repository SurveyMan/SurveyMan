package utils;

// this implemetnation owed to LtU and Stackoverflow for explanations on how
// to implement sum types:
// http://stackoverflow.com/questions/9975836/howto-simulate-haskells-either-a-b-in-java

import java.lang.RuntimeException;

abstract class Either<A, B> {

    private final A left;
    private final B right;

    Either<A,B> (A a) throws RuntimeException {
        if (this.right != null)
            throw RuntimeException("Tried making an Either a Both.");
        this.left = a;
        this.right = null;
    }

    Either<A,B> (B b) throws RuntimeException {
        if (this.left != null)
            throw RuntimeException("Tried making an Either a Both.");
        this.right = b;
        this.left = null;
    }

    public Object get() throws RuntimeException {
        if (this.left==null)
            return this.right;
        else if (this.right==null)
            return this.left;
        else throw RuntimeException("Both left and right are non-null.");
    }

}