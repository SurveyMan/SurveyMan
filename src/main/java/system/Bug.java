package system;

import java.lang.reflect.Method;

/**
 * Created with IntelliJ IDEA.
 * User: etosch
 * Date: 10/18/13
 * Time: 4:15 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Bug {

    public Object getCaller();
    public Method getLastAction();

}
