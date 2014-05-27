package system;

import com.amazonaws.mturk.service.exception.AccessKeyException;
import com.amazonaws.mturk.service.exception.InsufficientFundsException;
import input.csv.CSVLexer;
import input.csv.CSVParser;
import interstitial.*;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.dom4j.DocumentException;
import qc.IQCMetrics;
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
import util.ArgReader;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Runner {

    // everything that uses MturkResponseManager should probably use some parameterized type to make this more general
    // I'm hard-coding in the mturk stuff for now though.
    public static final Logger LOGGER = Logger.getLogger("SurveyMan");
    private static long timeSinceLastNotice = System.currentTimeMillis();
    private static FileAppender txtHandler;
    private static int totalHITsGenerated;
    public static AbstractResponseManager responseManager;
    public static ISurveyPoster surveyPoster;
    public static Library library;

    public static ArgumentParser makeArgParser(){
        ArgumentParser argumentParser = ArgumentParsers.newArgumentParser(Runner.class.getName(),true,"--").description("Posts surveys");
        for (Map.Entry<String, String> entry : ArgReader.getMandatoryAndDefault(Runner.class).entrySet())
            argumentParser.addArgument(entry.getKey()).required(true);
        for (Map.Entry<String, String> entry : ArgReader.getOptionalAndDefault(Runner.class).entrySet())
            argumentParser.addArgument(entry.getKey()).required(false).setDefault(entry.getValue());
        argumentParser.addArgument("program").nargs("?").required(true);
        return argumentParser;
    }

    public static void init(BackendType bt) throws UnknownBackendException {
        AbstractResponseManager rm;
        ISurveyPoster sp;
        Library lib;
        switch (bt) {
            case LOCALHOST:
                rm = new LocalResponseManager();
                sp = new LocalSurveyPoster();
                lib = new LocalLibrary();
                lib.init();
                break;
            case MTURK:
                rm = new MturkResponseManager();
                sp = new MturkSurveyPoster();
                lib = new MturkLibrary();
                lib.init();
                break;
            default:
                throw new UnknownBackendException(bt);
        }
        responseManager = rm;
        surveyPoster = sp;
        library = lib;
    }

    public static int recordAllTasksForSurvey(Survey survey, BackendType backendType) throws IOException, SurveyException, DocumentException {

        Record record = MturkResponseManager.getRecord(survey);
        int allHITs = record.getAllTasks().length;
        String hiturl = "", msg;
        int responsesAdded = 0;

        for (ITask hit : record.getAllTasks()) {
            hiturl = surveyPoster.makeTaskURL(hit);
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
        do {
            if (!interrupt.getInterrupt()) {
                surveyPoster.postSurvey(responseManager, record);
                AbstractResponseManager.chill(2);
            }
            AbstractResponseManager.chill(2);
        } while (stillLive(survey) && !interrupt.getInterrupt());
        surveyPoster.stopSurvey(responseManager, record, interrupt);
    }

    public static void runAll(String file, String sep, BackendType backendType)
        throws InvocationTargetException, SurveyException, IllegalAccessException, NoSuchMethodException, IOException, ParseException, InterruptedException, ClassNotFoundException, InstantiationException {

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
                Record record = new Record(survey, library, backendType);
                AbstractResponseManager.putRecord(survey, record);
                Thread writer = makeWriter(survey, interrupt);
                Thread responder = makeResponseGetter(survey, interrupt, backendType);
                responder.setPriority(Thread.MAX_PRIORITY);
                writer.start();
                responder.start();
                System.out.println("Target number of valid responses: " + record.library.props.get("numparticipants"));
                Runner.run(record, interrupt, backendType);
                //repl(interrupt, survey, record, backendType);
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
            throws IOException, SurveyException, InterruptedException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, ParseException, WebServerException, InstantiationException, ClassNotFoundException {

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

        ArgumentParser argumentParser = makeArgParser();
        Namespace ns;
        try {
            ns = argumentParser.parseArgs(args);

            BackendType backendType = BackendType.valueOf(ns.getString("backend"));

            if (backendType.equals(BackendType.LOCALHOST))
                Server.startServe();

            runAll(ns.getString("program"), ns.getString("separator"), backendType);

            if (backendType.equals(BackendType.LOCALHOST))
                Server.endServe();

        } catch (ArgumentParserException e) {
            argumentParser.printHelp();
        }
    }
}
