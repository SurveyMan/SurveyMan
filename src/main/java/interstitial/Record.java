package interstitial;

import org.apache.log4j.Logger;
import qc.QC;
import survey.Gensym;
import survey.Survey;
import java.io.File;
import java.io.IOException;
import java.util.*;
import survey.exceptions.SurveyException;

public class Record {

    final private static Logger LOGGER = Logger.getLogger(Record.class);
    final private static Gensym gensym = new Gensym("rec");

    public String outputFileName;
    final public Survey survey;
    public Library library;
    final public QC qc;
    final public String rid = gensym.next();
    //public QualificationType qualificationType;
    public List<ISurveyResponse> responses;
    public List<ISurveyResponse> botResponses;
    private Deque<ITask> tasks; // these should be hitids
    private String htmlFileName = "";
    public String hitTypeId = "";
    public BackendType backendType;

    public Record(final Survey survey, Library someLib, BackendType backendType) throws SurveyException {
        try {
            (new File(Library.OUTDIR)).mkdir();
            (new File("logs")).mkdir();
            File outfile = new File(String.format("%s%s%s_%s_%s.csv"
                    , Library.OUTDIR
                    , Library.fileSep
                    , survey.sourceName
                    , survey.sid
                    , Library.TIME));
            outfile.createNewFile();
            File htmlFileName = new File(String.format("%s%slogs%s%s_%s_%s.html"
                    , (new File("")).getAbsolutePath()
                    , Library.fileSep
                    , Library.fileSep
                    , survey.sourceName
                    , survey.sid
                    , Library.TIME));
            if (! htmlFileName.exists())
                htmlFileName.createNewFile();
            this.outputFileName = outfile.getCanonicalPath();
            this.htmlFileName = htmlFileName.getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.survey = survey;
        this.library = someLib; //new MturkLibrary();
        this.qc = new QC(survey);
        this.responses = new ArrayList<ISurveyResponse>();
        this.botResponses = new ArrayList<ISurveyResponse>();
        this.tasks = new ArrayDeque<ITask>();
        this.backendType = backendType;
        LOGGER.info(String.format("New record with id (%s) created for survey %s (%s)."
                , rid
                , survey.sourceName
                , survey.sid
        ));
    }

    public String getHtmlFileName() {
        return this.htmlFileName;
    }

    public void addNewTask(ITask task) {
        tasks.push(task);
        AbstractResponseManager.wakeup();
    }

    public ITask getLastTask(){
        return tasks.peekFirst();
    }

    public ITask[] getAllTasks() {
        return this.tasks.toArray(new ITask[tasks.size()]);
    }

    public List<String> getAllTaskIds() {
        List<String> retval = new ArrayList<String>();
        for (ITask task: this.tasks){
            retval.add(task.getTaskId());
        }
        return retval;
    }

}

