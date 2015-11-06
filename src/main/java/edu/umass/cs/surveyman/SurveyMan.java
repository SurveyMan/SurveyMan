package edu.umass.cs.surveyman;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import edu.umass.cs.surveyman.analyses.AbstractRule;
import edu.umass.cs.surveyman.analyses.DynamicAnalysis;
import edu.umass.cs.surveyman.analyses.StaticAnalysis;
import edu.umass.cs.surveyman.input.AbstractParser;
import edu.umass.cs.surveyman.input.csv.CSVLexer;
import edu.umass.cs.surveyman.input.csv.CSVParser;
import edu.umass.cs.surveyman.input.json.JSONParser;
import edu.umass.cs.surveyman.qc.Analyses;
import edu.umass.cs.surveyman.qc.Classifier;
import edu.umass.cs.surveyman.qc.respondents.RandomRespondent;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.ArgReader;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * Top level access point for the program. If you have downloaded the source, call <code>make package</code> from the
 * top level of the directory. Then call <code>java -jar target/surveyman-x.y.jar</code>, which will print out
 * instructions for running survey programs, the kinds of arguments you should use, etc.
 *
 * If you are using SurveyMan programmatically, look at the description of how main is called.
 */
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
    private static final String inputFormat = "inputformat";

    private SurveyMan()
    {
        // Never instantiate.
    }

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

    /**
     * Analyzes the survey. <b>Note that this does not set the rules used in static checking! You will need to call
     * AbstractRule.getDefaultRules() first.</b> This allows the user to extend or remove rules as needed.
     * @param survey The survey object.
     * @param analyses The type of analysis to run: static or dynamic.
     * @param classifier The type of classifier to use for bad actors.
     * @param n The total number of respondents to simulate (if running static analysis).
     * @param granularity The granularity of random respondents to increment by, for static analysis.
     * @param alpha The cutoff.
     * @param outputFile The file to write results to.
     * @param resultsfile The file containing results from running a survey (if running dynamic analyses).
     * @param smoothing Boolean indicating whether the system should use Laplace smoothing for question options.
     * @throws IOException
     * @throws com.github.fge.jsonschema.core.exceptions.ProcessingException
     * @throws SurveyException
     */
    public static void analyze(
            Survey survey,
            Analyses analyses,
            Classifier classifier,
            int n,
            double granularity,
            double alpha,
            String outputFile,
            String resultsfile,
            boolean smoothing)
            throws IOException, SurveyException, ProcessingException {
        LOGGER.info(survey.jsonize());
        OutputStream out = null;
        if (analyses.equals(Analyses.STATIC)) {
            StaticAnalysis.Report report = StaticAnalysis.staticAnalysis(survey, classifier, granularity, alpha, RandomRespondent.AdversaryType.UNIFORM);
            out = new FileOutputStream(outputFile);
            report.print(out);
        } else if (analyses.equals(Analyses.DYNAMIC)) {
            if (resultsfile==null || resultsfile.equals(""))
                throw new RuntimeException("Dynamic analyses require a results file.");
            List<DynamicAnalysis.DynamicSurveyResponse> responses = DynamicAnalysis.readSurveyResponses(survey, resultsfile);
            out = new FileOutputStream(outputFile);
            DynamicAnalysis.Report report = DynamicAnalysis.dynamicAnalysis(
                    survey, responses, classifier, smoothing, alpha);
            report.print(out);
        }
        out.close();
    }

    /**
     * The main entry point for the program. Running the jar with no arguments will call this function and then print
     * out a description of the arguments.
     *
     * If you would like to embed a SurveyMan program in another program, you will need to:
     * <ol>
     *    <li>Instantiate a lexer:<br/>
     *    <code>AbstractLexer lexer = new CSVLexer("my_survey.csv", ",");</code>
     *    </li>
     *    <li>Instantiate a parser:<br/>
     *    <code>AbstractParser parser = new CSVParser(lexer);</code>
     *    </li>
     *    <li>Parse the survey:<br/>
     *    <code>Survey survey = parser.parse();</code>
     *    </li>
     *    <li>Specify the rules you want to use to statically analyse the survey:<br/>
     *    <code>AbstractRule.getDefaultRules();</code>
     *    </li>
     *    <li>Then call analyze:<br/>
     *    <code>SurveyMan.analyze(survey, analyses, classifier, n, granularity, alpha, outputfile, resultsfile, smoothing);</code>
     *    </li>
     *    </ol>
     * @param args Arguments the top-level program. Execute <code>java -jar target/surveyman-x.y.jar</code> for guidance.
     */
    public static void main(String[] args) {
        ArgumentParser argumentParser = makeArgParser();
        Namespace ns;
        try {
            ns = argumentParser.parseArgs(args);
            Classifier classifier = Classifier.valueOf(((String) ns.get(classifierArg)).toUpperCase());
            Analyses analyses = Analyses.valueOf(((String) ns.get(analysisArg)).toUpperCase());
            int n = Integer.parseInt((String) ns.get(nArg));
            double granularity = Double.parseDouble((String) ns.get(granularityArg));
            double alpha = Double.parseDouble((String) ns.get(alphaArg));
            boolean smoothing = Boolean.parseBoolean((String) ns.get(smoothingArg));
            String outputfile = ns.getString(outputFileArg);
            String resultsfile = ns.getString(resultsfileArg);
            String inputformat = ns.getString(inputFormat);

            AbstractParser parser = null;
            if (inputformat.equals("csv")) {
                CSVLexer lexer = new CSVLexer((String) ns.get(surveyArg), (String) ns.get(separatorArg));
                parser = new CSVParser(lexer);
            } else if (inputformat.equals("json")) {
                parser = JSONParser.makeParser(ns.getString(surveyArg));
            }

            assert parser != null;
            Survey survey = parser.parse();
            AbstractRule.getDefaultRules();
            analyze(survey, analyses, classifier, n, granularity, alpha, outputfile, resultsfile, smoothing);
            System.out.println(String.format("Results found in file %s", ns.get("outputfile")));

        } catch (ArgumentParserException e) {
            System.out.println(e.getMessage());
            argumentParser.printHelp();
            LOGGER.error("FAILURE: "+e.getLocalizedMessage());
        } catch (SurveyException se) {
            System.err.println("FAILURE: " + se.getMessage());
            LOGGER.error(se);
            LOGGER.printf(Level.DEBUG, "%s", StringUtils.join(se.getStackTrace(), "\n"));
        } catch (Exception e) {
            e.printStackTrace();
        }
   }

}