package edu.umass.cs.surveyman;

import edu.umass.cs.surveyman.utils.ArgReader;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

public class SurveyMan {

    public static final Logger LOGGER = Logger.getLogger("SurveyMan");

    public static ArgumentParser makeArgParser(){
// move more of the setup into this method
        ArgumentParser argumentParser = ArgumentParsers.newArgumentParser(SurveyMan.class.getName(), true, "-").description("Posts surveys");
        argumentParser.addArgument("survey").required(true);
        for (Map.Entry<String, String> entry : ArgReader.getMandatoryAndDefault(SurveyMan.class).entrySet()) {
            String arg = entry.getKey();
            Argument a = argumentParser.addArgument("--" + arg)
                    .required(true)
                    .help(ArgReader.getDescription(arg));
            String[] c = ArgReader.getChoices(arg);
            if (c.length>0)
                a.choices(c);
        }
        for (Map.Entry<String, String> entry : ArgReader.getOptionalAndDefault(SurveyMan.class).entrySet()){
            String arg = entry.getKey();
            Argument a = argumentParser.addArgument("--" + arg)
                    .required(false)
                    .setDefault(entry.getValue())
                    .help(ArgReader.getDescription(arg));
            String[] c = ArgReader.getChoices(arg);
            if (c.length>0)
                a.choices(c);
        }
        return argumentParser;
    }

   public static void main(String[] args) {
       // parse survey
       // statically analyse
       // run simulations
       // LOGGING
       try {
           FileAppender txtHandler = new FileAppender(
                   new PatternLayout("%d{dd MMM yyyy HH:mm:ss,SSS}\t%-5p [%t]: %m%n"),
                   "logs/SurveyMan.log");
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
           Printer.updateVerbosity(Boolean.parseBoolean(ns.getString("verbose")));
           init(ns.getString("backend"), ns.getString("properties"), ns.getString("config"));
           if (backendType.equals(BackendType.LOCALHOST))
               Server.startServe();
           runAll(ns.getString("survey"), ns.getString("separator"));
           if (backendType.equals(BackendType.LOCALHOST))
               Server.endServe();
           String msg = String.format("Shutting down. Execute this program with args %s to repeat.", Arrays.toString(args));
           Printer.println(msg);
           LOGGER.info(msg);
       } catch (ArgumentParserException e) {
           System.err.println("FAILURE: "+e.getMessage());
           LOGGER.fatal(e);
           argumentParser.printHelp();
       }
   }
   }

}
