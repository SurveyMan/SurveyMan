package system.mturk.generators;

import system.Record;
import system.mturk.MturkLibrary;

public class HTML implements system.interfaces.HTML {

    public String getHTMLString() {
        return "<script type=\"text/javascript\" src=\"https://s3.amazonaws.com/mturk-public/externalHIT_v1.js\"></script>"
                + "<script type=\"text/javascript\" src=\"https://jqueryjs.googlecode.com/files/jquery-1.3.2.min.js\"></script>"
                + "<script type=\"text/javascript\" src=\"https://cdnjs.cloudflare.com/ajax/libs/underscore.js/1.5.2/underscore.js\"></script>"
                //+ "<script type=\"text/javscript\" src=\"http://davidbau.com/encode/seedrandom.min.js\"></script>"
                + "<script type=\"text/javascript\" src=\"https://cs.umass.edu/~etosch/seedrandom.min.js\"></script>"
                + "<script type=\"text/javascript\" src=\"https://cs.umass.edu/~etosch/randomize.js\"></script>"
                + "<script type=\"text/javascript\" src=\"https://cs.umass.edu/~etosch/ready.js\"></script>";
    }

    public String getActionForm(Record record){
        return ((MturkLibrary) record.library).EXTERNAL_HIT;
    }

}
