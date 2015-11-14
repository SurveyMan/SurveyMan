package edu.umass.cs.surveyman.utils;

import edu.umass.cs.surveyman.input.csv.CSVLexer;
import edu.umass.cs.surveyman.input.csv.CSVParser;

import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;


public class CSV2JSON {

    public static void main(String[] args) throws ArgumentParserException, InvocationTargetException, SurveyException, IllegalAccessException, NoSuchMethodException, IOException {
        try {
            ArgumentParser argumentParser = ArgumentParsers.newArgumentParser(
                    CSV2JSON.class.getName(), true, "-").description(
                    "Converts SurveyMan csv files to json.");
            argumentParser.addArgument("csv").required(true);
            argumentParser.addArgument("--sep").required(false).setDefault(",");
            argumentParser.addArgument("--encoding").required(false).setDefault("UTF-8");
            Namespace ns = argumentParser.parseArgs(args);
            CSVLexer csvLexer = new CSVLexer((String) ns.get("csv"), (String) ns.get("sep"), (String) ns.get("encoding"));
            CSVParser csvParser = new CSVParser(csvLexer);
            Survey survey = csvParser.parse();
            String json = survey.jsonize();
            BufferedWriter bw = new BufferedWriter(new FileWriter(survey.sourceName + ".json"));
            bw.write(json);
            bw.close();
        } catch (ArgumentParserException ape) {
            System.out.println(ape.getMessage());
            ape.getParser().printHelp();
        }
    }
}
