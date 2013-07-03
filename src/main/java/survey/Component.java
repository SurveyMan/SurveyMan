package survey;

import utils.Gensym;

public abstract class Component {

    // A component is one of the elements of a survey.
    // Components get unique ids
    // Components may be text or a url
    // Their layout may be controlled by an input css file

    // can't access my java Gensym, so I'll need to create a local one.
  private static final Gensym gensym = new Gensym("comp");
  public final String cid = gensym.next();
  
  public abstract boolean equals(Component c);

  public static void main(String[] args){
        // write test code here
  }

}




