package system.mturk;

import com.amazonaws.mturk.addon.HITProperties;
import com.amazonaws.mturk.requester.*;
import com.amazonaws.mturk.service.axis.RequesterService;
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
    final public MturkLibrary library;
    final public QC qc;
    final public String rid = gensym.next();
    public QualificationType qualificationType;
    public List<SurveyResponse> responses;
    public List<SurveyResponse> botResponses;
    private Deque<HIT> hits;
    private String htmlFileName = "";
    public String hitTypeId = "";

    private Record(final Survey survey, String hitTypeId) throws IOException, SurveyException {
        File outfile = new File(String.format("%s%s%s_%s_%s.csv"
                , MturkLibrary.OUTDIR
                , MturkLibrary.fileSep
                , survey.sourceName
                , survey.sid
                , Library.TIME));
        outfile.mkdirs();
        outfile.createNewFile();
        File htmlFileName = new File(String.format("%s%slogs%s%s_%s_%s.html"
                , (new File("")).getAbsolutePath()
                , Library.fileSep
                , Library.fileSep
                , survey.sourceName
                , survey.sid
                , Library.TIME));
        htmlFileName.createNewFile();
        this.outputFileName = outfile.getCanonicalPath();
        this.htmlFileName = htmlFileName.getCanonicalPath();
        this.survey = survey;
        this.library = new MturkLibrary();
        this.qc = new QC(survey);
        this.responses = new Vector<SurveyResponse>();
        this.botResponses = new Vector<SurveyResponse>();
        this.hits = new ArrayDeque<HIT>();
        this.hitTypeId = hitTypeId;
        LOGGER.info(String.format("New record with id (%s) created for survey %s (%s)."
                , rid
                , survey.sourceName
                , survey.sid
        ));
    }

    public Record(final Survey survey) throws IOException, SurveyException {
        this(survey, "");
        SurveyPoster.config.setServiceURL(this.library.MTURK_URL);
        SurveyPoster.service = new RequesterService(SurveyPoster.config);
        ResponseManager.service = SurveyPoster.service;
        //String hitTypeId = ResponseManager.registerNewHitType(this);
        //this.hitTypeId = hitTypeId;
    }

    public String getHtmlFileName() {
        return this.htmlFileName;
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

    public List<String> getAllHITIds() {
        List<String> retval = new ArrayList<String>();
        for (HIT hit : this.hits){
            retval.add(hit.getHITId());
        }
        return retval;
    }
}

