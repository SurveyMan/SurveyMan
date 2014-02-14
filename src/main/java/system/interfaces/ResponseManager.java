package system.interfaces;

import com.amazonaws.mturk.requester.HIT;
import survey.Survey;
import survey.SurveyException;
import system.Record;

import java.util.HashMap;

public abstract class ResponseManager {

    final public static int maxwaittime = 60;
    public static HashMap<String, Record> manager = new HashMap<String, Record>();

    public static void chill(int seconds){
        try {
            Thread.sleep(seconds*1000);
        } catch (InterruptedException e) {}
    }

    //public abstract int addResponses(Survey survey, HIT hit) throws SurveyException;
    //public List<Assignment> getAllAssignmentsForHIT(HIT hit);
}
