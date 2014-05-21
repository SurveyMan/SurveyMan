package system.localhost.generators;

import interstitial.IHTML;
import system.localhost.LocalLibrary;
import system.localhost.Server;
import interstitial.Record;

/**
 * Created by etosch on 2/13/14.
 */
public class LocalHTML implements IHTML {

    public static final int port = Server.frontPort;
    public static final String prefix = "http://localhost:" + port;

    public String getHTMLString() {
        String jsprefix = prefix + "/" + LocalLibrary.jshome;
        String node_mod = "/lib/node_modules";
        return  "<script type=\"text/javascript\" src=\""+ jsprefix + node_mod + "/jquery/dist/jquery.min.js\"></script>\n"
                + "<script type=\"text/javascript\" src=\""+ jsprefix + node_mod + "/underscore/underscore-min.js\"></script>\n"
                + "<script type=\"text/javascript\" src=\""+ jsprefix + node_mod + "/seedrandom/seedrandom.min.js\"></script>\n"
                + "<script type=\"text/javascript\" src=\"" + jsprefix + "/randomize.js\"></script>\n"
                + "<script type=\"text/javascript\" src=\"" + jsprefix + "/ready.js\"></script>\n"
                + "<script type=\"text/javascript\">\n"
                + " $.ajaxSetup({async:false});\n" +
                "var turkSetAssignmentID = function () { $.get(\"assignmentId\", function(_aid) { " +
                "console.log(\"Just pulled assignment Id : \" + _aid); " +
                "document.getElementById(\"assignmentId\").value = _aid.trim(); " +
                "aid = _aid;" +
                "}); }; \n"
                + "</script>\n";
    }

    public String getActionForm(Record record) {
        return "";
    }

    public LocalHTML(){}
}
