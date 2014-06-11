package system;

import system.mturk.MturkLibrary;
import util.Printer;
import util.Slurpie;
import interstitial.*;
import survey.Survey;
import survey.exceptions.SurveyException;
import system.localhost.LocalResponseManager;
import system.localhost.LocalTask;
import system.mturk.MturkResponseManager;
import system.mturk.MturkTask;

import java.io.*;

public class JobManager {

    static class JobSynchronizationException extends SystemException {
        public JobSynchronizationException(String jobID) {
            super("Could not find past job with id " + jobID);
        }
    }

    public static void recordBonus(double bonus, ISurveyResponse sr, Survey survey) throws IOException {
        String entry = String.format("%s,%s,%f\n", sr.workerId(), survey.sourceName, bonus);
        dump(Library.BONUS_DATA, entry, true);
    }

    public static boolean bonusPaid(ISurveyResponse sr, Survey survey) throws IOException {
        String data = Slurpie.slurp(Library.BONUS_DATA);
        for (String line : data.split("\n")){
            String[] pieces = line.split(",");
            if (pieces[0].equals(sr.workerId()) && pieces[1].equals(survey.sourceName)) {
                Printer.println("BONUS PAID");
                return true;
            }
        }
        return false;
    }

    public static String makeJobID(Survey survey) {
        return survey.sourceName+"_"+survey.sid+"_"+ Library.TIME;
    }

    public static void dump(String filename, String s, boolean append) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename, append));
        writer.write(s);
        writer.close();
    }

    public static void dump(String filename, String s) throws IOException {
        dump(filename, s, true);
    }

    public static boolean addToUnfinishedJobsList(Survey survey, Record record, BackendType backendType) {
        StringBuilder data = new StringBuilder();
        data.append(makeJobID(survey)).append(",").append(backendType.name());
        for (ITask task : record.getAllTasks())
            data.append(",").append(task.getTaskId());
        data.append("\n");

        try {
            dump(Library.UNFINISHED_JOB_FILE, data.toString());
            return true;
        } catch (IOException ex) {
            Runner.LOGGER.warn(ex);
        }
        return false;
    }

    public static boolean saveParametersAndSurvey(Survey survey, Record record) {
        try {
            String jobID = makeJobID(survey);
            String dir = Library.STATEDATADIR + Library.fileSep + jobID + Library.fileSep;
            // make a directory with this name
            (new File(dir)).mkdir();
            FileWriter out = null;
            try {
                out = new FileWriter(dir + jobID + ".params");
                record.library.props.store(out, "");
            } finally {
                if(out != null) out.close();
            }
            dump(dir + jobID + ".csv", Slurpie.slurp(survey.source));
            return true;
        } catch (IOException ex) {
            Runner.LOGGER.warn(ex);
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
            Printer.println(record.outputFileName);
            ISurveyResponse sr = new SurveyResponse("");
            sr.readSurveyResponses(record.survey, new FileReader(record.outputFileName));
        } catch (IOException io) {
            Runner.LOGGER.info(io);
        }
    }

    public static int populateTasks(String jobId, Record r, BackendType backendType) throws SystemException, SurveyException {
        try {
            String unfinished = Slurpie.slurp(Library.UNFINISHED_JOB_FILE);
            for (String line : unfinished.split("\n")) {
                String[] data = line.split(",");
                if (data[0].equals(jobId)) {
                    for (int i = 2 ; i < data.length ; i++) {
                        switch (backendType) {
                            case MTURK:
                                MturkTask mturkTask = (MturkTask) new MturkResponseManager(new MturkLibrary()).getTask(data[i]);
                                mturkTask.setRecord(r);
                                break;
                            case LOCALHOST:
                                LocalTask localTask = (LocalTask) new LocalResponseManager().getTask(data[i]);
                                localTask.setRecord(r);
                        }
                    }
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

            StringBuilder writeMe = new StringBuilder();
            for (String line : unfinished.split("\n")) {
                String thisJobId = line.split(",")[0];
                if (! thisJobId.equals(jobId))
                    writeMe.append(line).append('\n');
            }
            JobManager.dump(Library.UNFINISHED_JOB_FILE, writeMe.toString(), false);
        } catch (IOException ex) {
            Runner.LOGGER.warn(ex);
        }
    }
}
