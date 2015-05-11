package edu.umass.cs.surveyman.survey;

import edu.umass.cs.surveyman.survey.exceptions.BlockException;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.*;

public class BranchMap implements Map<SurveyDatum, Block>, Serializable {

    private int count;
    private int initSize = 5;
    private SurveyDatum[] keys = new SurveyDatum[initSize];
    private Block[] vals = new Block[initSize];

    protected String jsonize() {
        Iterator<Entry<SurveyDatum, Block>> entrySet = this.entrySet().iterator();
        if (!entrySet.hasNext())
            return "";
        Entry<SurveyDatum, Block> entry = entrySet.next();
        String oid = entry.getKey().getId();
        String bid = entry.getValue() == null ? "null" : "\"" + entry.getValue().getStrId()+ "\"";
        StringBuilder s = new StringBuilder(String.format("\"%s\" : %s", oid, bid));
        while (entrySet.hasNext()) {
            entry = entrySet.next();
            oid = entry.getKey().getId();
            bid = entry.getValue() == null ? "null" : "\"" + entry.getValue().getStrId() + "\"";
            s.append(String.format(", \"%s\" : %s", oid, bid));
        }
        return "{" + s.toString() + "}";
    }

    @Override
    public int size() {
        return count;
    }

    @Override
    public boolean isEmpty() {
        return count == 0;
    }

    @Override
    public boolean containsKey(Object o) {
        assert o instanceof SurveyDatum;
        for (SurveyDatum c : keys)
            if (c.equals(o))
                return true;
        return false;
    }

    @Override
    public boolean containsValue(Object o) {
        assert o instanceof Block;
        for (Block b : vals)
            if (b.equals(o))
                return true;
        return false;
    }

    @Override
    public Block get(Object o) {
        assert o instanceof SurveyDatum;
        for (int i = 0; i < keys.length; i++)
            if (keys[i].equals(o))
                return vals[i];
        throw new RuntimeException(new BlockException(String.format(
                "No destination block found for SurveyDatum %s", o.toString())));
    }

    @Override
    public Block put(SurveyDatum surveyDatum, Block block) {
        for (int i = 0; i < count; i++) {
            if (keys[i].equals(surveyDatum)) {
                keys[i] = surveyDatum;
                vals[i] = block;
            }
        }
        if (count == keys.length) {
            SurveyDatum[] tmp1 = keys;
            keys = new SurveyDatum[count * 2];
            System.arraycopy(tmp1, 0, keys, 0, tmp1.length);
            Block[] tmp2 = vals;
            vals = new Block[count * 2];
            System.arraycopy(tmp2, 0, vals, 0, tmp2.length);
        }
        keys[count] = surveyDatum;
        vals[count] = block;
        count++;
        return block;
    }

    @Override
    public Block remove(Object o) {
        throw new RuntimeException("This feature not implemented");
    }

    @Override
    public void putAll(Map<? extends SurveyDatum, ? extends Block> map) {
        for (Entry<? extends SurveyDatum, ? extends Block> e : map.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    @Override
    public void clear() {
        count = 0;
        keys = new SurveyDatum[initSize];
        vals = new Block[initSize];
    }

    @Override
    public Set<SurveyDatum> keySet() {
        Set<SurveyDatum> retval = new HashSet<SurveyDatum>();
        for (int i = 0; i < count; i++) {
            retval.add(keys[i]);
        }
        return retval;
    }

    @Override
    public Collection<Block> values() {
        Set<Block> retval = new HashSet<Block>();
        for (int i = 0; i < count; i++) {
            retval.add(vals[i]);
        }
        return retval;
    }

    @Override
    public Set<Entry<SurveyDatum, Block>> entrySet() {
        Set<Entry<SurveyDatum, Block>> set = new HashSet<Entry<SurveyDatum, Block>>();
        for (int i = 0; i < count; i++)
            set.add(new AbstractMap.SimpleImmutableEntry<SurveyDatum, Block>(keys[i], vals[i]));
        return set;
    }

    @Override
    public boolean equals(Object o) {
        assert o instanceof Map;
        boolean equality = this.count == ((Map) o).size();
        for (Object e : ((Map) o).entrySet()) {
            SurveyDatum c = (SurveyDatum) ((Entry) e).getKey();
            Block b = (Block) ((Entry) e).getValue();
            equality &= this.get(c).equals(b);
        }
        return equality;
    }

    @Override
    public int hashCode() {
        int hc = 0;
        for (int i = 0; i < count; i++) {
            hc += keys[i].hashCode() + vals[i].hashCode();
        }
        return hc;
    }

    @Override
    public String toString()
    {
        List<String> strings = new ArrayList<String>();
        for (Entry<SurveyDatum, Block> entry : this.entrySet()) {
            strings.add(String.format(
                    "%s -> %s",
                    entry.getKey().toString(),
                    entry.getValue().getStrId()));
        }
        return StringUtils.join(strings, "\n");
    }
}
