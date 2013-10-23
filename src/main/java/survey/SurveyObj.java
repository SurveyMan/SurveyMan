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
public abstract class SurveyObj implements Comparable {

    public int index;
    protected static Random rng = new Random();

    @Override
    public int compareTo(Object o) {
        return this.index - ((SurveyObj) o).index;
    }
}
