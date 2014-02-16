package system;

import gui.SurveyMan;
import survey.Survey;
import system.interfaces.Task;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class JobManager {

    public static final String UNFINISHED_JOB_FILE = Library.DIR + Library.fileSep + ".unfinished";

    public static String makeJobID(Survey survey) {
        return survey.sourceName+"_"+survey.sid+"_"+Library.TIME;
    }

    public static void dump(String filename, String s) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true));
        writer.write(s);
        writer.close();
    }



    public static boolean addToUnfinishedJobsList(Survey survey, Record record) {
        String data = makeJobID(survey);
        for (Task task : record.getAllTasks())
            data = data + "," + task.getTaskId();
        data = data + "\n";
        try {
            dump(UNFINISHED_JOB_FILE, data);
            return true;
        } catch (IOException ex) {
            SurveyMan.LOGGER.warn(ex);
        }
        return false;
    }

    public static boolean saveParametersAndSurvey(Survey survey, Record record) {
        try {
            String jobID = makeJobID(survey);
            String prefix = Library.DIR+Library.fileSep+".data"+Library.fileSep;
            // make a directory with this name
            (new File(prefix+jobID)).mkdir();
            String dir = prefix+jobID+Library.fileSep;
            record.library.props.store(new FileWriter(dir+jobID+".params"), "");
            dump(dir+jobID+".csv", Slurpie.slurp(survey.source));
            return true;
        } catch (IOException ex) {
            SurveyMan.LOGGER.warn(ex);
        }
        return false;
    }

}
