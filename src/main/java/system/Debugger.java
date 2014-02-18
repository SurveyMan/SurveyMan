package system;

import java.util.LinkedList;

/**
 * Created with IntelliJ IDEA.
 * User: etosch
 * Date: 10/18/13
 * Time: 2:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class Debugger {

    public static LinkedList<Bug> registeredBugs = new LinkedList<Bug>();

    public static void addBug(Bug bug) {
        registeredBugs.add(bug);
    }

}
