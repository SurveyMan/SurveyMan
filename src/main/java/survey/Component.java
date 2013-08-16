package survey;

import utils.Gensym;

public abstract class Component {

    // A component is one of the elements of a survey.
    // Components get unique ids
    // Components may be text or a url
    // Their layout may be controlled by an input css file

    private static final Gensym gensym = new Gensym("comp");
    public final String cid = gensym.next();
    public int index;

    public abstract boolean equals(Component c);

    @Override
    public String toString() {
    return "cid:" + cid + " index:" + index;
    }
}




