package edu.umass.cs.surveyman;

import edu.umass.cs.surveyman.analyses.AbstractSurveyResponse;
import edu.umass.cs.surveyman.analyses.DynamicAnalysis;
import edu.umass.cs.surveyman.analyses.StaticAnalysis;
import edu.umass.cs.surveyman.analyses.AbstractRule;
import edu.umass.cs.surveyman.input.csv.CSVLexer;
import edu.umass.cs.surveyman.input.csv.CSVParser;
import edu.umass.cs.surveyman.qc.Analyses;
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
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public class SurveyMan {

    /**
     * If SurveyMan is not called as a command line program, then this class simply provides a single instance of the
     * logger.
     */
    public static final Logger LOGGER = LogManager.getLogger(SurveyMan.class.getName());
    private static final String classifierArg = "classifier";
    private static final String nArg = "n";
    private static final String surveyArg = "survey";
    private static final String separatorArg = "separator";
    private static final String granularityArg = "granularity";
    private static final String outputFileArg = "outputfile";
    private static final String alphaArg = "alpha";
    private static final String analysisArg = "analysis";
    private static final String resultsfileArg = "resultsfile";
    private static final String smoothingArg = "smoothing";

    private static ArgumentParser makeArgParser(){
        ArgumentParser argumentParser = ArgumentParsers.newArgumentParser(
                SurveyMan.class.getName(), true, "-").description(
                "Performs static analysis and dynamic analysis on surveys according to the SurveyMan language.");
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
        OutputStream out;
        try {
            ns = argumentParser.parseArgs(args);
            Classifier classifier = Classifier.valueOf(((String) ns.get(classifierArg)).toUpperCase());
            Analyses analyses = Analyses.valueOf(((String) ns.get(analysisArg)).toUpperCase());
            int n = Integer.parseInt((String) ns.get(nArg));
            double granularity = Double.parseDouble((String) ns.get(granularityArg));
            double alpha = Double.parseDouble((String) ns.get(alphaArg));
            boolean smoothing = Boolean.parseBoolean((String) ns.get(smoothingArg));
            CSVLexer lexer = new CSVLexer((String) ns.get(surveyArg), (String) ns.get(separatorArg));
            CSVParser parser = new CSVParser(lexer);
            Survey survey = parser.parse();
            AbstractRule.getDefaultRules();
            LOGGER.info(survey.jsonize());
            if (analyses.equals(Analyses.STATIC)) {
                StaticAnalysis.Report report = StaticAnalysis.staticAnalysis(survey, classifier, n, granularity, alpha);
                out = new FileOutputStream((String) ns.get(outputFileArg));
                report.print(out);
                out.close();
            } else if (analyses.equals(Analyses.DYNAMIC)) {
                String resultsfile = ns.getString(resultsfileArg);
                if (resultsfile==null || resultsfile.equals(""))
                    throw new ArgumentParserException("Dynamic analyses require a results file.", argumentParser);
                List<AbstractSurveyResponse> responses = DynamicAnalysis.readSurveyResponses(survey, resultsfile);
                out = new FileOutputStream((String) ns.get(outputFileArg));
                DynamicAnalysis.Report report = DynamicAnalysis.dynamicAnalysis(
                        survey, responses, classifier, smoothing, alpha);
                report.print(out);
                out.close();
            }
       } catch (ArgumentParserException e) {
            System.out.println(e.getMessage());
            argumentParser.printHelp();
            LOGGER.error("FAILURE: "+e.getLocalizedMessage());
       } catch (SurveyException se) {
           System.err.println("FAILURE: "+se.getMessage());
           LOGGER.error(se);
       } catch (Exception e) {
           e.printStackTrace();
       }
   }

}