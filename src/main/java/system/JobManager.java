package system;

import gui.SurveyMan;
import survey.Survey;
import survey.SurveyException;
import survey.SurveyResponse;
import system.interfaces.ResponseManager;
import system.interfaces.Task;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class JobManager {

    static class JobSynchronizationException extends SystemException {
        public JobSynchronizationException(String jobID) {
            super("Could not find past job with id " + jobID);
        }
    }

    public static String makeJobID(Survey survey) {
        return survey.sourceName+"_"+survey.sid+"_"+Library.TIME;
    }

    public static void dump(String filename, String s) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true));
        writer.write(s);
        writer.close();
    }

    public static boolean addToUnfinishedJobsList(Survey survey, Record record, BackendType backendType) {
        String data = makeJobID(survey) + "," + backendType.name();
        for (Task task : record.getAllTasks())
            data = data + "," + task.getTaskId();
        data = data + "\n";
        try {
            dump(Library.UNFINISHED_JOB_FILE, data);
            return true;
        } catch (IOException ex) {
            SurveyMan.LOGGER.warn(ex);
        }
        return false;
    }

    public static boolean saveParametersAndSurvey(Survey survey, Record record) {
        try {
            String jobID = makeJobID(survey);
            String dir = Library.STATEDATADIR + Library.fileSep + jobID + Library.fileSep;
            // make a directory with this name
            (new File(dir)).mkdir();
            record.library.props.store(new FileWriter(dir + jobID + ".params"), "");
            dump(dir + jobID + ".csv", Slurpie.slurp(survey.source));
            return true;
        } catch (IOException ex) {
            SurveyMan.LOGGER.warn(ex);
        }
        return false;
    }

    public static BackendType getBackendTypeByJobID(String jobId) throws SystemException {
        try {
            String unfinished = Slurpie.slurp(Library.UNFINISHED_JOB_FILE);
            for (String line : unfinished.split("\n")) {
                String[] data = line.split(",");
                if (data[0].equals(jobId))
                    return BackendType.valueOf(data[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new JobSynchronizationException(jobId);
    }

    public static void addOldResponses(String jobId, Record record) throws SurveyException {
        record.outputFileName = Library.OUTDIR + Library.fileSep + jobId + ".csv";
        try {
            String[] responses = Slurpie.slurp(record.outputFileName).split("\n");
            SurveyResponse.readSurveyResponses(record.survey, record.outputFileName);
        } catch (IOException io) {
            SurveyMan.LOGGER.info(io);
        }
    }

    public static int populateTasks(String jobId, Record r, ResponseManager responseManager) throws SystemException, SurveyException {
        try {
            String unfinished = Slurpie.slurp(Library.UNFINISHED_JOB_FILE);
            for (String line : unfinished.split("\n")) {
                String[] data = line.split(",");
                if (data[0].equals(jobId)) {
                    for (int i = 2 ; i < data.length ; i++)
                        responseManager.addTaskToRecordByTaskId(r, data[i]);
                    // update record
                    addOldResponses(jobId, r);
                    return data.length - 2;
                }
            }
            throw new JobSynchronizationException(jobId);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static void removeUnfinished(String jobId) {
        try {
            String unfinished = Slurpie.slurp(Library.UNFINISHED_JOB_FILE);
            String writeMe = "";
            for (String line : unfinished.split("\n")) {
                String thisJobId = line.split(",")[0];
                if (! thisJobId.equals(jobId))
                    writeMe += String.format("%s\n", line);
            }
            JobManager.dump(Library.UNFINISHED_JOB_FILE, writeMe);
        } catch (IOException ex) {
            SurveyMan.LOGGER.warn(ex);
        }
    }
}
