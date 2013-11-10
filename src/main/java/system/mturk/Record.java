package system.mturk;

import com.amazonaws.mturk.requester.*;
import org.apache.log4j.Logger;
import qc.QC;
import survey.Survey;
import survey.SurveyResponse;
import system.Gensym;
import system.Library;

import java.io.File;
import java.io.IOException;
import java.util.*;
import survey.SurveyException;

/**
 * Record is the class used to hold instance information about a currently running survey.
 */
public class Record {

    final private static Logger LOGGER = Logger.getLogger(Record.class);
    final private static Gensym gensym = new Gensym("rec");

    final public String outputFileName;
    final public Survey survey;
    final public Properties parameters;
    final public QC qc;
    final public String rid = gensym.next();
    public QualificationType qualificationType;
    public List<SurveyResponse> responses;
    public List<SurveyResponse> botResponses;
    private Deque<HIT> hits;
    private String htmlFileName = "";
    public String hitTypeId = "";

    public Record(final Survey survey, final Properties parameters, QualificationType qualificationType, String hitTypeId)
            throws IOException, SurveyException {
        File outfile = new File(String.format("%s%s%s_%s_%s.csv"
                , MturkLibrary.OUTDIR
                , MturkLibrary.fileSep
                , survey.sourceName
                , survey.sid
                , Library.TIME));
        outfile.createNewFile();
        this.outputFileName = outfile.getCanonicalPath();
        this.survey = survey;
        this.parameters = parameters;
        this.qc = new QC(survey);
        this.responses = new Vector<SurveyResponse>();
        this.botResponses = new Vector<SurveyResponse>();
        this.hits = new ArrayDeque<HIT>();
        this.qualificationType = qualificationType;
        this.hitTypeId = hitTypeId;
        LOGGER.info(String.format("New record with id (%s) created for survey %s (%s)."
                , rid
                , survey.sourceName
                , survey.sid
            ));
        ResponseManager.manager.notify();
    }

    public String getHtmlFileName() {
        return this.htmlFileName;
    }

    public synchronized void setHtmlFileName(String filename){
        this.htmlFileName = filename;
    }

    public Record(final Survey survey, final Properties parameters)
            throws IOException, SurveyException {
        this(survey, parameters, null, "");
    }

    public void addNewHIT(HIT hit) {
        hits.push(hit);
    }

    public HIT getLastHIT(){
        return hits.peekFirst();
    }

    public HIT[] getAllHITs() {
        return this.hits.toArray(new HIT[hits.size()]);
    }

    public void resetHITList(){
        this.hits = new ArrayDeque<HIT>();
    }

    public List<String> getAllHITIds() {
        List<String> retval = new ArrayList<String>();
        for (HIT hit : this.hits){
            retval.add(hit.getHITId());
        }
        return retval;
    }

//    public synchronized Record copy() throws IOException, SurveyException {
//        LOGGER.warn(String.format("COPYING RECORD %s for survey %s (%s)", rid, survey.sourceName, survey.sid));
//        Record r = new Record(this.survey, this.parameters);
//        // don't expect responses to be removed or otherwise modified, so it's okay to just copy them over
//        for (SurveyResponse sr : responses)
//            r.responses.add(sr);
//        // don't expect HITs to be removed either
//        // double check to make sure this is being added in the proper direction
//        r.hits.addAll(this.hits);
//        r.htmlFileName = this.htmlFileName;
//        r.qualificationType = this.qualificationType;
//        return r;
//    }
}

