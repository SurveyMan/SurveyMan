package survey;

import java.lang.String;

public class Block {

    private static final Gensym gensym = new Gensym("block");
    public final String bid = gensym.next();

    public final int[] id;
    public List<Question> questions;

    public void randomize(){
        // randomizes the question list
        return;
    }

    public static void main(String[] args){
        // write test code here
    }
}