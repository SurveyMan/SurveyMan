package system;

import csv.CSVLexer;
import csv.CSVParser;
import java.io.FileNotFoundException;
import java.io.IOException;
import survey.Survey;
import system.mturk.XMLGenerator;

public class Debugger {


    public static boolean noLoops(Survey s){
        return false;
    }

    public static boolean properBlockOrdering(Survey s){
        return false;
    }

    // add more

    public static void driverLoop(){
        //do stuff
    }
    
    public static void main(String[] args) throws FileNotFoundException, IOException {
        String filename, seperator = ",";
        if (args.length < 1) {
            System.out.println("You must provide at least a file name to parse. See the README for more information.");
            System.exit(0);
        } else if (args.length > 1) 
            seperator = args[1].split("=")[1];
        filename = args[0];
        Survey survey = CSVParser.parse(filename, seperator);
        System.out.println(XMLGenerator.getXMLString(survey));
    }
    
}