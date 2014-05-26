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

    public static final Logger LOGGER = Logger.getLogger("SurveyMan");
    private static long timeSinceLastNotice = System.currentTimeMillis();
    private static FileAppender txtHandler;
    private static int totalHITsGenerated;
    public static HashMap<BackendType, AbstractResponseManager> responseManagers = new HashMap<BackendType, AbstractResponseManager>();
    public static HashMap<BackendType, ISurveyPoster> surveyPosters = new HashMap<BackendType, ISurveyPoster>();
    public static HashMap<BackendType, Library> libraries = new HashMap<BackendType, Library>();

    public static String PROGNAME = "surveyman";
    public static String SURVEYPATH = "survey-path";
    public static String SURVEYPROPSPATH = "survey-props-path";
    public static String BACKENDTYPE = "backend-type";
    public static String MTCONFIG = "mturk-config";
    public static String SEPARATOR = "separator";
    public static String VERBOSE = "verbose";

    public static ArgParse initArgs() {
        String program_name = PROGNAME;
        List<String> mandatory_args = new ArrayList<String>() {{ add(SURVEYPATH); }};
        HashMap<String,ArgParse.ArgType> optional_flags = new HashMap<String,ArgParse.ArgType>() {{
            put(SURVEYPROPSPATH, ArgParse.ArgType.KEYVALUE);
            put(BACKENDTYPE, ArgParse.ArgType.KEYVALUE);
            put(MTCONFIG, ArgParse.ArgType.KEYVALUE);
            put(SEPARATOR, ArgParse.ArgType.KEYVALUE);
            put(VERBOSE, ArgParse.ArgType.KEY);
        }};
        HashMap<String,String> arg_usage = new HashMap<String,String>() {{
            put(SURVEYPATH, "Path to the survey CSV file, relative from the current working directory.");
            put(SURVEYPROPSPATH, "Path to a Java properties file containing survey metadata, relative from the current working directory. If omitted, default is '~/surveyman/params.properties'.");
            put(BACKENDTYPE, "One of the following backends: MTURK | LOCALHOST. If omitted, default is LOCALHOST.");
            put(MTCONFIG, "Path to a Java properties file containing MTurk credentials, relative from the current working directory. If omitted, default is '~/surveyman/mturk_config'.");
            put(SEPARATOR, "The survey CSV field separator.  Should be a single character or special character like '\\t'. If omitted, default is ','.");
            put(VERBOSE, "Produces verbose output. If omitted, default is no verbose output.");
        }};
        HashMap<String,String> defaults = new HashMap<String,String>() {{
            put(SURVEYPROPSPATH, Library.PARAMS);
            put(BACKENDTYPE, "LOCALHOST");
            put(MTCONFIG, MturkLibrary.CONFIG);
            put(SEPARATOR, ",");
            put(VERBOSE, "false");
        }};

        String examples = String.format("Example usage:%n%s --%s=MTURK --%s=~/mturk_config %s%n",
                                        PROGNAME, BACKENDTYPE, MTCONFIG, "MySurvey.csv");

        return new ArgParse(program_name, mandatory_args, optional_flags, arg_usage, defaults, examples);
    }

    private static class Args {
        public String surveyPath;
        public String surveyPropsPath;
        public String separator;
        public String mtconfig;
        public BackendType backendType;
        public Boolean verbose;
        public Args(HashMap<String,String> args) {
            this.surveyPath = args.get(SURVEYPATH);
            this.surveyPropsPath = args.get(SURVEYPROPSPATH);
            this.separator = args.get(SEPARATOR);
            this.backendType = BackendType.valueOf(args.get(BACKENDTYPE));
            this.mtconfig = args.get(MTCONFIG);
            this.verbose = Boolean.valueOf(args.get(VERBOSE));
        }
        @Override
        public String toString() {
            return String.format("Arguments:%n" +
                            "\tsurveyPath = %s%n" +
                            "\tsurveyPropsPath = %s%n" +
                            "\tseparator = %s%n" +
                            "\tbackendType = %s%n" +
                            "\tmtconfig = %s%n" +
                            "\tverbose = %s%n",
                    surveyPath,
                    surveyPropsPath,
                    separator,
                    backendType,
                    mtconfig,
                    verbose);
        }
    }

    public static void init(BackendType bt, Properties surveyProps, Properties mtConfig) throws UnknownBackendException {
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
                lib = new MturkLibrary(surveyProps, mtConfig);
                break;
            default:
                throw new UnknownBackendException(bt);
        }
        responseManagers.put(bt, rm);
        surveyPosters.put(bt, sp);
        libraries.put(bt, lib);
    }

    public static void init() throws UnknownBackendException {
        init(BackendType.LOCALHOST,
             input.PropLoader.loadFromFile(Library.PARAMS, LOGGER),
             input.PropLoader.loadFromFile(MturkLibrary.CONFIG, LOGGER));
    }

    public static int recordAllTasksForSurvey(Survey survey, BackendType backendType)
            throws IOException, SurveyException, DocumentException {

        Record record = MturkResponseManager.getRecord(survey);
        int allHITs = record.getAllTasks().length;
        String hiturl = "", msg;
        int responsesAdded = 0;

        for (ITask hit : record.getAllTasks()) {
            ISurveyPoster sp = surveyPosters.get(backendType);
            hiturl = sp.makeTaskURL(hit);
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
            System.out.println("Total Tasks generated: "+record.getAllTasks().length);
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
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (SurveyException e) {
                            e.printStackTrace();
                        }
                    }
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
                    //AbstractResponseManager.putRecord(survey, null);
                    System.out.println("done.");
                }
            }
        };
    }

    public static void run(final Record record, final BoxedBool interrupt, final BackendType backendType)
            throws SurveyException, IOException, ParseException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        Survey survey = record.survey;
        AbstractResponseManager responseManager = responseManagers.get(backendType);
        ISurveyPoster surveyPoster = surveyPosters.get(backendType);
        do {
            if ( !interrupt.getInterrupt() ) {
                surveyPoster.postSurvey(responseManager, record);
                AbstractResponseManager.chill(2);
            }
            AbstractResponseManager.chill(2);
        } while (stillLive(survey) && !interrupt.getInterrupt());
        surveyPoster.stopSurvey(responseManager, record, interrupt);
    }

    public static void runAll(Args a)
        throws InvocationTargetException,
               SurveyException,
               IllegalAccessException,
               NoSuchMethodException,
               IOException,
               ParseException,
               InterruptedException,
               ClassNotFoundException,
               InstantiationException {
        try {
            init(a.backendType,
                 input.PropLoader.loadFromFile(a.surveyPropsPath, LOGGER),
                 input.PropLoader.loadFromFile(a.mtconfig, LOGGER));
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
                responder.join();
                writer.join();
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
        // process arguments
        ArgParse p = initArgs();
        Args a = new Args(p.processArgs(args));

        // print parsed flags
        if (Boolean.valueOf(a.verbose) == true) {
            System.err.println(a.toString());
        }

        // start server (if necessary), run main loop, stop server (if necessary)
        if (a.backendType.equals(BackendType.LOCALHOST)) { Server.startServe(); }
        runAll(a);
        if (a.backendType.equals(BackendType.LOCALHOST)) { Server.endServe(); }
    }
}
