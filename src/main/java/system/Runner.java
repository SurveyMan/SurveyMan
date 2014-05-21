package system;

import com.amazonaws.mturk.service.exception.AccessKeyException;
import com.amazonaws.mturk.service.exception.InsufficientFundsException;
import input.csv.CSVLexer;
import input.csv.CSVParser;
import interstitial.*;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.dom4j.DocumentException;
import survey.Rules;
import survey.Survey;
import survey.exceptions.SurveyException;
import system.localhost.LocalResponseManager;
import system.localhost.LocalSurveyPoster;
import system.localhost.Server;
import system.localhost.server.WebServerException;
import system.mturk.MturkResponseManager;
import system.mturk.MturkSurveyPoster;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class Runner {

    static enum ReplAction { QUIT, CANCEL; }

    // everything that uses MturkResponseManager should probably use some parameterized type to make this more general
    // I'm hard-coding in the mturk stuff for now though.
    public static final Logger LOGGER = Logger.getLogger("SurveyMan");
    private static FileAppender txtHandler;
    private static int totalHITsGenerated;
    public static HashMap<BackendType, AbstractResponseManager> responseManagers = new HashMap<BackendType, AbstractResponseManager>();
    public static HashMap<BackendType, ISurveyPoster> surveyPosters = new HashMap<BackendType, ISurveyPoster>();
    public static void init(BackendType bt) throws UnknownBackendException {
        AbstractResponseManager rm;
        ISurveyPoster sp;
        switch (bt) {
            case LOCALHOST:
                rm = new LocalResponseManager();
                sp = new LocalSurveyPoster();
                break;
            case MTURK:
                rm = new MturkResponseManager();
                sp = new MturkSurveyPoster();
                break;
            default:
                throw new UnknownBackendException(bt);
        }
        responseManagers.put(bt, rm);
        surveyPosters.put(bt, sp);
    }

    public static int recordAllTasksForSurvey(Survey survey, BackendType backendType) throws IOException, SurveyException, DocumentException {
        Record record = MturkResponseManager.getRecord(survey);
        int allHITs = record.getAllTasks().length;
        String hiturl = "", msg;
        int responsesAdded = 0;
        for (ITask hit : record.getAllTasks()) {
            hiturl = surveyPosters.get(backendType).makeTaskURL(hit);
            AbstractResponseManager responseManager = responseManagers.get(backendType);
            responsesAdded = responseManager.addResponses(survey, hit);
        }
        msg = String.format("Polling for responses for Tasks at %s (%d total)"
                , hiturl
                , record.responses.size());
        System.out.println(msg);
        if (allHITs > totalHITsGenerated) {
            System.out.println("total Tasks generated: "+record.getAllTasks().length);
            totalHITsGenerated = allHITs;
            System.out.println(msg);
        }
        LOGGER.info(msg);
        return responsesAdded;
    }

    public static Thread makeResponseGetter(final Survey survey, final BoxedBool interrupt, final BackendType backendType){
        // grab responses for each incomplete survey in the responsemanager
        return new Thread(){
            @Override
            public void run(){
                int waittime = 1;
                while (!interrupt.getInterrupt()){
                    System.out.println(String.format("Checking for responses in %s", backendType));
                    AbstractResponseManager responseManager = responseManagers.get(backendType);
                    while(!interrupt.getInterrupt()){
                        try {
                            int n = recordAllTasksForSurvey(survey, backendType);
                            if (n > 0)
                                waittime = 2;
                        } catch (IOException e) {
                            e.printStackTrace(); throw new RuntimeException(e);
                        } catch (SurveyException e) {
                            e.printStackTrace(); throw new RuntimeException(e);
                        } catch (DocumentException e) {
                            e.printStackTrace(); throw new RuntimeException(e);
                        }
                        AbstractResponseManager.chill(waittime);
                        if (waittime > AbstractResponseManager.maxwaittime)
                            waittime = 1;
                        else waittime *= 2;
                    }
                    // if we're out of the loop, expire and process the remaining HITs
                    System.out.println("\n\tDANGER ZONE\n");
                    AbstractResponseManager.chill(3);
                    Record record = null;
                    try {
                        record = responseManager.getRecord(survey);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (SurveyException e) {
                        e.printStackTrace();
                    }
                    assert(responseManager!=null);
                    for (ITask task : record.getAllTasks()){
                        boolean expiredAndAdded = false;
                        while (! expiredAndAdded) {
                            try {
                                responseManager.makeTaskUnavailable(task);
                                responseManager.addResponses(survey, task);
                                expiredAndAdded = true;
                            } catch (Exception e) {
                                System.out.println("something in the response getter thread threw an error.");
                                e.printStackTrace();
                                AbstractResponseManager.chill(1);
                            }
                        }
                    }
                    AbstractResponseManager.removeRecord(record);
                }
            }
        };
    }

    public static boolean stillLive(Survey survey) throws IOException, SurveyException {
        Record record = AbstractResponseManager.getRecord(survey);
        if (record==null) {
            return false;
        }
        boolean done = record.qc.complete(record.responses, record.library.props);
        return ! done;
    }

    public static void writeResponses(Survey survey, Record record){
        for (ISurveyResponse sr : record.responses) {
            synchronized(sr) {
                if (!sr.isRecorded()) {
                    BufferedWriter bw = null;
                    System.out.println("writing "+sr.srid());
                    try {
                        String sep = ",";
                        System.out.println(record.outputFileName);
                        File f = new File(record.outputFileName);
                        bw = new BufferedWriter(new FileWriter(f, true));
                        if (! f.exists() || f.length()==0)
                            bw.write(ResponseWriter.outputHeaders(survey));
                        String txt = ResponseWriter.outputSurveyResponse(survey, sr);
                        System.out.println(txt);
                        bw.write(ResponseWriter.outputSurveyResponse(survey, sr));
                        sr.setRecorded(true);
                        bw.close();
                        System.out.println("Wrote one response");
                    } catch (IOException ex) {
                        System.out.println(ex);
                        LOGGER.warn(ex);
                    } finally {
                        try {
                            if (bw != null) bw.close();
                        } catch (IOException ex) {
                            LOGGER.warn(ex);
                        }
                    }
                }
            }
        }
    }

    public static Thread makeWriter(final Survey survey, final BoxedBool interrupt){
        //writes hits that correspond to current jobs in memory to their files
        return new Thread(){
            @Override
            public void run(){
                Record record = null;
                do {
                    while (true) {
                        try {
                            while (AbstractResponseManager.getRecord(survey) == null) {
                                try {
                                    System.out.println("waiting...");
                                    AbstractResponseManager.waitOnManager();
                                } catch (InterruptedException ie) { LOGGER.warn(ie); }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            continue;
                        } catch (SurveyException e) {
                            e.printStackTrace();
                            continue;
                        }
                        break;
                    }
                    try {
                        record = AbstractResponseManager.getRecord(survey);
                        writeResponses(survey, record);
                        AbstractResponseManager.chill(3);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (SurveyException e) {
                        e.printStackTrace();
                    }
                } while (!interrupt.getInterrupt());
                // clean up
                synchronized (record) {
                    System.out.print("Writing straggling data...");
                    try {
                        record.wait();
                    } catch (InterruptedException e) { LOGGER.warn(e); }
                    writeResponses(survey, record);
                    AbstractResponseManager.putRecord(survey, null);
                    System.out.println("done.");
                }
            }
        };
    }

    public static void run(final Record record, final BoxedBool interrupt, final BackendType backendType)
            throws SurveyException, IOException, ParseException {
        Survey survey = record.survey;
        // make sure survey is well formed
        Rules.ensureBranchForward(survey, null);
        Rules.ensureBranchTop(survey, null);
        Rules.ensureCompactness(survey);
        Rules.ensureNoDupes(survey);
        Rules.ensureBranchParadigms(survey, null);
        Rules.ensureNoTopLevelRandBranching(survey);
        Rules.ensureSampleHomogenousMaps(survey);
        Rules.ensureExclusiveBranching(survey);
        AbstractResponseManager responseManager = responseManagers.get(backendType);
        ISurveyPoster surveyPoster = surveyPosters.get(backendType);
        do {
            if (!interrupt.getInterrupt() && surveyPoster.postMore(responseManager, record)) {
                List<ITask> tasks = surveyPoster.postSurvey(responseManager, record);
                System.out.println("num tasks posted from Runner.run " + tasks.size());
                AbstractResponseManager.chill(2);
            }
            AbstractResponseManager.chill(2);
        } while (stillLive(survey) && !interrupt.getInterrupt());
        surveyPoster.stopSurvey(responseManager, record, interrupt);
    }

    private static void repl(BoxedBool interrupt, Survey survey, Record record, BackendType backendType){
        boolean quit = false;
        Scanner in = new Scanner(new InputStreamReader(System.in));
        PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out));
        while (!quit) {
            String cmd = in.nextLine();
            switch (ReplAction.valueOf(cmd)) {
                case QUIT:
                    quit = true;
                    interrupt.setInterrupt(true);
                    JobManager.addToUnfinishedJobsList(survey, record, backendType);
                    out.print("Saving state information before exiting...");
                    break;
                case CANCEL:
                    interrupt.setInterrupt(true);
                    JobManager.addToUnfinishedJobsList(survey, record, backendType);
                    out.print("Saving state information...");
                    break;
            }
        }
    }

    public static void runAll(String file, String sep, BackendType backendType)
            throws InvocationTargetException, SurveyException, IllegalAccessException, NoSuchMethodException, IOException, ParseException, InterruptedException {
        try {
            init(backendType);
        } catch (UnknownBackendException ube) {
            System.out.println(ube.getMessage());
            System.exit(1);
        }

        while (true) {
            try {
                BoxedBool interrupt = new BoxedBool(false);
                CSVParser csvParser = new CSVParser(new CSVLexer(file, sep));
                Survey survey = csvParser.parse();
                // create and store the record
                Record record = new Record(survey, new Library(survey), backendType);
                AbstractResponseManager.putRecord(survey, record);
                Thread writer = makeWriter(survey, interrupt);
                Thread responder = makeResponseGetter(survey, interrupt, backendType);
                responder.setPriority(Thread.MAX_PRIORITY);
                writer.start();
                responder.start();
                Runner.run(record, interrupt, backendType);
                repl(interrupt, survey, record, backendType);
                writer.join();
                responder.join();
                System.exit(0);
            } catch (InsufficientFundsException ife) {
                System.out.println("Insufficient funds in your Mechanical Turk account. Would you like to:\n" +
                        "[1] Add more money to your account and retry\n" +
                        "[2] Quit\n");
                int i = 0;
                while(i!=1 && i!=2){
                    System.out.println("Type number corresponding to preference: ");
                    i = new Scanner(System.in).nextInt();
                    if (i==2)
                        System.exit(1);
                }
            } catch (AccessKeyException aws) {
                System.out.println(String.format("There is a problem with your access keys: %s; Exiting...", aws.getMessage()));
                MturkResponseManager.chill(2);
                System.exit(0);
            }
        }
    }

    public static void main(String[] args)
            throws IOException, SurveyException, InterruptedException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, ParseException, WebServerException {


        if (args.length!=3) {
            System.err.println("USAGE: <survey.csv> <sep> <backend>\r\n"
                + "survey.csv  the relative path to the survey csv file from the current location of execution.\r\n"
                + "sep         the field separator (should be a single char or 2-char special char, e.g. \\t\r\n"
                + "backend     one of the following: MTURK | LOCALHOST"
            );
            System.exit(-1);
        }

        // LOGGING
        try {
            txtHandler = new FileAppender(new PatternLayout("%d{dd MMM yyyy HH:mm:ss,SSS}\t%-5p [%t]: %m%n"), "logs/Runner.log");
            txtHandler.setAppend(true);
            LOGGER.addAppender(txtHandler);
        }
        catch (IOException io) {
            System.err.println(io.getMessage());
            System.exit(-1);
        }

        String file = args[0];
        String sep = args[1];
        BackendType backendType = BackendType.valueOf(args[2]);

        Server.startServe();

        runAll(file, sep, backendType);

        Server.endServe();
    }
}
