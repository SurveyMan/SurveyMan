package system.mturk.generators;

import interstitial.Record;
import interstitial.IHTML;

public class MturkHTML implements IHTML {

    public String getHTMLString() {
        return "<script type=\"text/javascript\" src=\"https://s3.amazonaws.com/mturk-public/externalHIT_v1.js\"></script>"
                + "<script type=\"text/javascript\" src=\"https://jqueryjs.googlecode.com/files/jquery-1.3.2.min.js\"></script>"
                + "<script type=\"text/javascript\" src=\"https://cdnjs.cloudflare.com/ajax/libs/seedrandom/2.3.4/seedrandom.min.js\"></script>"
                + "<script type=\"text/javascript\" src=\"https://cdnjs.cloudflare.com/ajax/libs/underscore.js/1.6.0/underscore.js\"></script>"
                + "<script type=\"text/javascript\" src=\"https://cs.umass.edu/~etosch/randomize.js\"></script>"
                + "<script type=\"text/javascript\" src=\"https://cs.umass.edu/~etosch/ready.js\"></script>";
    }

    public String getActionForm(Record record){
        return record.library.getActionForm();
    }

}
