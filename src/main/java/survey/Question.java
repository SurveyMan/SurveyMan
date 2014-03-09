package survey;

import java.util.*;
import java.util.regex.Pattern;

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

    public String quid;
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
    public Pattern freetextPattern;
    public String freetextDefault;
    public boolean permitBreakoff = true;

    public static String makeQuestionId(int row, int col) {
        return String.format("q_%d_%d", row, col);
    }

    public Question(int row, int col){
        this.quid = makeQuestionId(row, col);
    }

    public Component getOptById(String oid) throws SurveyException {
        if (options.containsKey(oid))
            return options.get(oid);
        throw new OptionNotFoundException(oid, this.quid);
    }

    public Component[] getOptListByIndex() throws SurveyException {
        if (freetext) return new Component[0];
        Component[] opts = new Component[options.size()];
        for (Component c : options.values())
            if (c.index > options.size())
                throw new MalformedOptionException(String.format("Option \r\n{%s}\r\n has an index that exceeds max index %d"
                        , c.toString()
                        , options.size() - 1));
            else if (opts[c.index] != null)
                throw new MalformedOptionException(String.format("Options \r\n{%s}\r\n and \r\n{%s}\r\n have the same index. (Entries (%d, %d) and (%d, %d)."
                        , opts[c.index]
                        , c.toString()
                        , opts[c.index].getSourceRow(), opts[c.index].getSourceCol()
                        , c.getSourceRow(), c.getSourceCol()
                        )
                    );
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

    public Block getFurthestAncestor(Survey survey) throws Survey.BlockNotFoundException {
        if (this.block.isTopLevel())
            return this.block;
        else return survey.getBlockById(new int[]{ this.block.getBlockId()[0] });
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
