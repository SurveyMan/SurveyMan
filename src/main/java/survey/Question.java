package survey;

import java.util.*;

public class Question extends SurveyObj{


    public static class MalformedOptionException extends SurveyException {
        public MalformedOptionException(String msg) {
            super(msg);
        }
    }

    public static class OptionNotFoundException extends SurveyException {
        public OptionNotFoundException(String oid, String quid){
            super(String.format("Option %s not found in Question %s", oid, quid));
        }
    }

    public final String quid;
    public List<Component> data = new ArrayList<Component>();
    public Map<String, Component> options;
    public Map<Component, Block> branchMap = new HashMap<Component, Block>();
    public List<Integer> sourceLineNos = new ArrayList<Integer>();
    public Map<String, String> otherValues = new HashMap<String, String>();
    public Block block;
    public Boolean exclusive;
    public Boolean ordered;
    public Boolean randomize;
    public Boolean freetext;

    public static String makeQuestionId(int row, int col) {
        return String.format("q_%d_%d", row, col);
    }

    public Question(int row, int col){
        this.quid = makeQuestionId(row, col);
    }

    public void randomize() throws SurveyException {
        // randomizes options, if permitted
        Component[] opts = getOptListByIndex();
        if (randomize)
            if (ordered && rng.nextFloat()>0.5) {
                // reverse
                for (int i = 0 ; i < opts.length ; i++)
                    opts[i].index = opts.length-1-i;
            } else if (!ordered) {
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
        if (options.containsKey(oid))
            return options.get(oid);
        throw new OptionNotFoundException(oid, this.quid);
    }
        
    public Component[] getOptListByIndex() throws SurveyException {
        Component[] opts = new Component[options.size()];
        for (Component c : options.values())
            if (c.index > options.size())
                throw new MalformedOptionException(String.format("Option \r\n{%s}\r\n has an index that exceeds max index %d"
                        , c.toString()
                        , options.size() - 1));
//            else if (opts[c.index] != null)
//                throw new MalformedOptionException(String.format("Options \r\n{%s}\r\n and \r\n{%s}\r\n have the same index."
//                        , opts[c.index]
//                        , c.toString()));
            else
                opts[c.index] = c;
         return opts;
    }

    public boolean before(Question q) {
        int[] myBLockID = this.block.id;
        for (int i = 0 ; i < myBLockID.length ; i++) {
            if (i >= q.block.id.length)
                return false; // can't say it's strictly before
            else if (myBLockID[i] < q.block.id[i])
                return true;
        }
        return false;
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
                && this.randomize.equals(q.randomize);
    }

}
