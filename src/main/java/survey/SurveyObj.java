package survey;

import java.util.List;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: etosch
 * Date: 10/23/13
 * Time: 12:05 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class SurveyObj implements Comparable<SurveyObj> {

    public int index;
    protected static Random rng = new Random();

    @Override
    public int compareTo(SurveyObj o) {
        System.out.println("Comparing?");
        return this.index - o.index;
    }
}
