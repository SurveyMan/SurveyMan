package survey;

import java.net.URL;
import qc.QCMetric;
import java.util.List;

import utils.Gensym;

public class Survey {

    private static final Gensym gensym = new Gensym("survey");

    public String sid = gensym.next();
    public URL splashPage;
    public List<Question> questions; //top level list of questions
    public QCMetric qc;
    public Block[] blocks;

    public void randomize(){
    }

    @Override
    public String toString() {
        String str = "Survey id " + sid + "\n";
        if (blocks.length > 0) {
            for (int i = 0 ; i < blocks.length ; i ++)
                str = str + "\n" + blocks[i].toString();
        } else {
            for (Question q : questions)
                str = str +"\n" + q.toString();
        }
        return str;
    }

    public static void main(String[] args){
        // write test code here
    }


}