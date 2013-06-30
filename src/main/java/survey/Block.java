package survey;

import java.lang.String;
import utils.Gensym;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Block {

    private static final Gensym gensym = new Gensym("block");
    public final String bid = gensym.next();

    public String strId;
    public int[] id = null;
    // source lines come from the questions
    public List<Integer> sourceLines = new ArrayList<Integer>();
    public List<Question> questions = new ArrayList<Question>();
    public Block[] subBlocks = null;

    public void randomize(){
    }
    
    public boolean equals(Block b) {
        return Arrays.equals(this.id, b.id);
    }
    
   @Override
    public String toString() {
        String indent = "";
        if (id!=null) {
            for (int i = 0 ; i < id.length ; i++)
                indent += "\t";
        }
        indent = "\n" + indent;
        String str = strId + ":" + indent;
        for (Question q : questions)
            str = str + "\n" + indent + q.toString();
        if (subBlocks!=null) {
            for (int i = 0 ; i < subBlocks.length ; i ++)
                str = str + subBlocks[i].toString();
        }
        return str;
    }
   
    public static void main(String[] args){
        // write test code here
    }
}