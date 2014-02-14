package system.localhost;

import com.amazonaws.mturk.requester.HIT;
import org.dom4j.DocumentException;
import org.xml.sax.SAXException;
import survey.Survey;
import survey.SurveyException;
import survey.SurveyResponse;
import system.Gensym;
import system.Record;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by etosch on 2/13/14.
 */
public class ResponseManager extends system.interfaces.ResponseManager {

    private static final Gensym workerIds = new Gensym("w");

    public List<String> getNewAnswers() {
        return null;
    }

    public int addResponses(Survey survey, HIT hit) {
        Record record = manager.get(survey.sid);
        List<String> newAnswers = getNewAnswers();
        List<SurveyResponse> responsesToAdd = new ArrayList<SurveyResponse>();
        try {
            for (String xmlStringAnswer : newAnswers) {
                SurveyResponse sr = new SurveyResponse(survey
                        , workerIds.next()
                        , xmlStringAnswer
                        , record
                        , null
                );
                responsesToAdd.add(sr);
            }
        } catch (SurveyException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        record.responses.addAll(responsesToAdd);
        return responsesToAdd.size();
    }
}
