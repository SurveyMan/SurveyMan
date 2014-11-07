package edu.umass.cs.surveyman;

import edu.umass.cs.surveyman.analyses.StaticAnalysis;
import edu.umass.cs.surveyman.analyses.AbstractRule;
import edu.umass.cs.surveyman.input.csv.CSVLexer;
import edu.umass.cs.surveyman.input.csv.CSVParser;
import edu.umass.cs.surveyman.qc.Classifier;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.ArgReader;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.FileOutputStream;
import java.util.Map;

public class SurveyMan {

    /**
     * If SurveyMan is not called as a command line program, then this class simply provides a single instance of the
     * logger.
     */
    public static final Logger LOGGER = LogManager.getLogger(SurveyMan.class.getName());
    private static String classifierArg = "classifier";
    private static String nArg = "n";
    private static String surveyArg = "survey";
    private static String separatorArg = "separator";
    private static String granularityArg = "granularity";

    private static ArgumentParser makeArgParser(){
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
        for (Map.Entry<String, String> entry : ArgReader.getOptionalAndDefault(SurveyMan.class).entrySet()) {
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
       ArgumentParser argumentParser = makeArgParser();
       Namespace ns;
       try {
           ns = argumentParser.parseArgs(args);
           Classifier classifier = Classifier.valueOf(((String) ns.get(classifierArg)).toUpperCase());
           int n = Integer.parseInt((String) ns.get(nArg));
           double granularity = Double.parseDouble(granularityArg);
           CSVLexer lexer = new CSVLexer((String) ns.get(surveyArg), (String) ns.get(separatorArg));
           CSVParser parser = new CSVParser(lexer);
           Survey survey = parser.parse();
           AbstractRule.getDefaultRules();
           LOGGER.info(survey.jsonize());
           StaticAnalysis.Report report = StaticAnalysis.staticAnalysis(survey, classifier, n, granularity);
           report.print(new FileOutputStream("out"));
       } catch (ArgumentParserException e) {
           argumentParser.printHelp();
       } catch (SurveyException se) {
           System.err.println("FAILURE: "+se.getMessage());
           LOGGER.error(se);
       } catch (Exception e) {
           e.printStackTrace();
       }
   }

}