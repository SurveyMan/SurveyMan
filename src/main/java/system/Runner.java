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
import system.localhost.LocalLibrary;
import system.localhost.LocalResponseManager;
import system.localhost.LocalSurveyPoster;
import system.localhost.Server;
import system.localhost.server.WebServerException;
import system.mturk.MturkLibrary;
import system.mturk.MturkResponseManager;
import system.mturk.MturkSurveyPoster;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.*;

public class Runner {

    static enum ReplAction { QUIT, CANCEL; }

    // everything that uses MturkResponseManager should probably use some parameterized type to make this more general
    // I'm hard-coding in the mturk stuff for now though.
    public static final Logger LOGGER = Logger.getLogger("SurveyMan");
    private static long timeSinceLastNotice = System.currentTimeMillis();
    private static FileAppender txtHandler;
    private static int totalHITsGenerated;
    public static HashMap<BackendType, AbstractResponseManager> responseManagers = new HashMap<BackendType, AbstractResponseManager>();
    public static HashMap<BackendType, ISurveyPoster> surveyPosters = new HashMap<BackendType, ISurveyPoster>();
    public static HashMap<BackendType, Library> libraries = new HashMap<BackendType, Library>();

    public static void init(BackendType bt, Properties surveyProps) throws UnknownBackendException {
        AbstractResponseManager rm;
        ISurveyPoster sp;
        Library lib;
        switch (bt) {
            case LOCALHOST:
                rm = new LocalResponseManager();
                sp = new LocalSurveyPoster();
                lib = new LocalLibrary(surveyProps);
                break;
            case MTURK:
                rm = new MturkResponseManager();
                sp = new MturkSurveyPoster();
                lib = new MturkLibrary(surveyProps);
                break;
            default:
                throw new UnknownBackendException(bt);
        }
        responseManagers.put(bt, rm);
        surveyPosters.put(bt, sp);
        libraries.put(bt, lib);
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

        if (System.currentTimeMillis() - timeSinceLastNotice > 60000) {
            System.out.println(msg);
            LOGGER.info(msg);
            timeSinceLastNotice = System.currentTimeMillis();
        }

        if (allHITs > totalHITsGenerated) {
            System.out.println("total Tasks generated: "+record.getAllTasks().length);
            totalHITsGenerated = allHITs;
            System.out.println(msg);
        }

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
                    Record record = null;
                    assert(responseManager!=null);
                    synchronized (responseManager){
                        System.out.println("\n\tDANGER ZONE\n");
                        try {
                            record = responseManager.getRecord(survey);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (SurveyException e) {
                            e.printStackTrace();
                        }
                    }
                    assert(record!=null);
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

    public static boolean stillLive(Survey survey) throws IOException, SurveyException, ClassNotFoundException, IllegalAccessException, InstantiationException {
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
                    writeResponses(survey, record);
                    AbstractResponseManager.putRecord(survey, null);
                    System.out.println("done.");
                }
            }
        };
    }

    public static void run(final Record record, final BoxedBool interrupt, final BackendType backendType)
            throws SurveyException, IOException, ParseException, IllegalAccessException, InstantiationException, ClassNotFoundException {
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

    public static void runAll(Args a)
        throws InvocationTargetException, SurveyException, IllegalAccessException, NoSuchMethodException, IOException, ParseException, InterruptedException, ClassNotFoundException, InstantiationException {

        try {
            init(a.backendType, input.PropLoader.loadFromFile(a.surveyParamsPath, LOGGER));
        } catch (UnknownBackendException ube) {
            System.out.println(ube.getMessage());
            System.exit(-1);
        }

        while (true) {
            try {
                BoxedBool interrupt = new BoxedBool(false);
                CSVParser csvParser = new CSVParser(new CSVLexer(a.surveyPath, a.separator));
                Survey survey = csvParser.parse();
                // create and store the record
                Record record = new Record(survey, libraries.get(a.backendType), a.backendType);
                AbstractResponseManager.putRecord(survey, record);
                Thread writer = makeWriter(survey, interrupt);
                Thread responder = makeResponseGetter(survey, interrupt, a.backendType);
                responder.setPriority(Thread.MAX_PRIORITY);
                writer.start();
                responder.start();
                System.out.println("Target number of valid responses: " + record.library.props.get("numparticipants"));
                Runner.run(record, interrupt, a.backendType);
                //repl(interrupt, survey, record, backendType);
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
                        System.exit(-1);
                }
            } catch (AccessKeyException aws) {
                System.out.println(String.format("There is a problem with your access keys: %s; Exiting...", aws.getMessage()));
                MturkResponseManager.chill(2);
                System.exit(0);
            }
        }
    }

    public static void Usage(String because) {
        System.err.printf("ERROR: %s%n%n", because);
        System.err.printf("USAGE: [OPTION...] <survey.csv>%n"
                        + "survey.csv\t\tthe relative path to the survey csv file from the current location of execution.%n%n"
                        + "SurveyMan also accepts a number of optional parameters:%n"
                        + "-p=<survey properties>\trelative path to a Java properties file containing survey metadata.%n"
                        + "-s=<separator>\t\tthe field separator (should be a single char or 2-char special char, e.g. \\t%n"
                        + "-b=<backend>\t\tone of the following backends: MTURK | LOCALHOST.  LOCALHOST is the default.%n"
                        + "-v\t\t\tprints verbose output.%n%n"
        );
        System.exit(-1);
    }

    public static String Join(String[] strs, String delim) {
        return Arrays.toString(strs).replace(", ", delim).replaceAll("[\\[\\]]", "");
    }

    private static void ArgMapAdd(HashMap<String,String> argmap, String optname, String key, String value) {
        if (argmap.containsKey(optname)) {
            Usage(String.format("Duplicate option: \"%s\".", key));
        }
        argmap.put(optname, value);
    }

    private static void DefArgMapAdd(HashMap<String,String> argmap, String optname, String value) {
        if (!argmap.containsKey(optname)) {
            argmap.put(optname, value);
        }
    }

    public static Args processArgs(String[] argarray) {
        String args = Join(argarray, " ");
        if (argarray.length == 0) { Usage("Missing arguments."); }

        Scanner sc = new Scanner(args);
        HashMap<String,String> argmap = new HashMap<String, String>();

        // get key-value pairs
        String s;
        while((s = sc.findInLine("(-[a-z]\\s*=\\s*\\S+)|(-[a-z])")) != null) {
            String[] pair = s.split("=");
            String key = pair[0];

            if (pair.length == 1) {
                // single-key args
                switch(key) {
                    case "-v":
                        ArgMapAdd(argmap, "verbose", key, "true");
                        break;
                    default:
                        Usage("Unrecognized option.");
                }
            } else {
                // key-value args
                String value = pair[1];
                switch (key) {
                    case "-b":
                        ArgMapAdd(argmap, "backend", key, value);
                        break;
                    case "-p":
                        ArgMapAdd(argmap, "properties", key, value);
                        break;
                    case "-s":
                        ArgMapAdd(argmap, "separator", key, value);
                        break;
                    default:
                        Usage("Unrecognized option.");
                }
            }
        }

        // get survey input file name
        String surveyPath = sc.findInLine("\\S+");
        if (surveyPath == null) { Usage("No survey path. Note that options must precede the survey path."); }
        argmap.put("survey", surveyPath);

        // check for anything remaining
        if (sc.hasNext()) { Usage("Unrecognized tokens after survey path argument."); }

        sc.close();

        // add default options if not specified
        DefArgMapAdd(argmap, "backend", "LOCALHOST");
        DefArgMapAdd(argmap, "properties", "params.properties");
        DefArgMapAdd(argmap, "separator", ",");
        DefArgMapAdd(argmap, "verbose", "false");

        Args a = new Args(argmap.get("survey"),
                          argmap.get("properties"),
                          argmap.get("separator"),
                          argmap.get("backend"),
                          argmap.get("verbose"));

        // print arguments if verbose
        if (a.verbose) { System.err.println(a.toString()); }

        return a;
    }

    private static class Args {
        public String surveyPath;
        public String surveyParamsPath;
        public String separator;
        public BackendType backendType;
        public Boolean verbose;
        public Args(String surveyPath,
                    String surveyParamsPath,
                    String separator,
                    String backendType,
                    String verbose) {
            this.surveyPath = surveyPath;
            this.surveyParamsPath = surveyParamsPath;
            this.separator = separator;
            this.backendType = BackendType.valueOf(backendType);
            this.verbose = Boolean.valueOf(verbose);
        }
        @Override
        public String toString() {
            return String.format("Arguments:%n" +
                                 "\tsurveyPath = %s%n" +
                                 "\tsurveyParamsPath = %s%n" +
                                 "\tseparator = %s%n" +
                                 "\tbackendType = %s%n" +
                                 "\tverbose = %s%n",
                                 surveyPath,
                                 surveyParamsPath,
                                 separator,
                                 backendType,
                                 verbose);
        }
    }

    public static void InitLogger() {
        try {
            txtHandler = new FileAppender(new PatternLayout("%d{dd MMM yyyy HH:mm:ss,SSS}\t%-5p [%t]: %m%n"), "logs/Runner.log");
            txtHandler.setAppend(true);
            LOGGER.addAppender(txtHandler);
        }
        catch (IOException io) {
            System.err.println(io.getMessage());
            System.exit(-1);
        }
    }

    public static void main(String[] args)
            throws IOException,
                   SurveyException,
                   InterruptedException,
                   NoSuchMethodException,
                   IllegalAccessException,
                   InvocationTargetException,
                   ParseException,
                   WebServerException,
                   InstantiationException,
                   ClassNotFoundException {
        InitLogger();
        Args a = processArgs(args);
        Server.startServe();
        runAll(a);
        Server.endServe();
    }
}
