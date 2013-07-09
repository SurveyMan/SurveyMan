package survey;

import java.util.*;

import utils.Gensym;

public class Question {

    protected static Random rng = new Random();
    private static final Gensym gensym = new Gensym("q");
    public final String quid = gensym.next();

    public List<Component> data = new ArrayList<Component>();
    public Map<String, Component> options;
    public List<Integer> sourceLineNos = new ArrayList<Integer>();
    public Map<String, String> otherValues = new HashMap<String, String>();
    public Block block;
    public Question branchLeft;
    public Question branchRight;
    public Boolean exclusive;
    public Boolean ordered;
    public Boolean perturb;
    public int index;


    public void randomize() throws SurveyException {
        // randomizes options, if permitted
        Component[] opts = getOptListByIndex();
        if (perturb)
            if (ordered && rng.nextBoolean()) {
                // reverse
                for (int i = 0 ; i < opts.length ; i++)
                    opts[i].index = opts.length-1-i;
            } else {
                // fisher-yates shuffle - descending makes the rng step less verbose
                for (int i = opts.length ; i > 0 ; i--) {
                    int j = rng.nextInt(i);
                    int temp = opts[j].index;
                    opts[j].index = opts[i-1].index;
                    opts[i-1].index = temp;
                }
            }
    }
    
    public Component getOptById(String oid) throws SurveyException {
        for (Component c : options.values()) {
            if (c.cid.equals(oid))
                return c;
        }
        throw new OptionNotFoundException(oid, this.quid);
    }
        
    public Component[] getOptListByIndex() throws SurveyException {
        Component[] opts = new Component[options.size()];
        for (Component c : options.values())
            if (c.index > options.size())
                throw new MalformedOptionException(String.format("Option \r\n{%s}\r\n has an index that exceeds max index %d"
                        , c.toString()
                        , options.size() - 1));
            else if (opts[c.index] != null)
                throw new MalformedOptionException(String.format("Options \r\n{%s}\r\n and \r\n{%s}\r\n have the same index."
                        , opts[c.index]
                        , c.toString()));
            else
                opts[c.index] = c;
         return opts;
    }

    public int[] histogram() {
        return new int[0];
    }

    @Override
    public String toString() {
        return "[(" + index + ") " + data.toString() + "]";
    }

    public boolean equals(Question q){
        return this.data.equals(q.data)
                && this.options.equals(q.options)
                && this.block.equals(q.block)
                && this.exclusive.equals(q.exclusive)
                && this.ordered.equals(q.ordered)
                && this.perturb.equals(q.perturb);
    }
    
    public static void main(String[] args){
        // write test code here
    }
}

class MalformedOptionException extends SurveyException {
    public MalformedOptionException(String msg) {
        super(msg);
    }
}

class OptionNotFoundException extends SurveyException {
    public OptionNotFoundException(String oid, String quid){
        super(String.format("Option %s not found in Question %s", oid, quid));
    }
}