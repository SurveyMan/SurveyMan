package system.mturk;

import java.io.FileNotFoundException;
import java.io.IOException;
import survey.Survey;

class HTMLGenerator{
    
    public static String stringify(Survey survey) {
        return "";
    }
    
    public static String getHTMLString(Survey survey) throws FileNotFoundException, IOException{
        return String.format(Slurpie.slurp(".metadata/HTMLSkeleton.html")
                , survey.encoding
                , Slurpie.slurp(".metadata/JSSkeleton.js")
                , stringify(survey));
    }

    public static void main(String[] args){
    }

}