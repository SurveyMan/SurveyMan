package system.localhost.generators;

import system.interfaces.IHTML;
import system.localhost.LocalLibrary;
import system.localhost.Server;
import system.Record;

/**
 * Created by etosch on 2/13/14.
 */
public class LocalHTML implements IHTML {

    public static final int port = Server.frontPort;
    public static final String prefix = "http://localhost:" + port;

    public String getHTMLString() {
        String jsprefix = prefix + "/" + LocalLibrary.jshome;
        return  "<script type=\"text/javascript\" src=\""+ jsprefix +"/node_modules/jquery/dist/jquery.min.js\"></script>\n"
                + "<script type=\"text/javascript\" src=\""+ jsprefix +"/node_modules/underscore/underscore-min.js\"></script>\n"
                + "<script type=\"text/javascript\" src=\""+jsprefix+"/node_modules/seedrandom/seedrandom.min.js\"></script>\n"
                + "<script type=\"text/javascript\" src=\"" + jsprefix + "/randomize.js\"></script>\n"
                +"<!--BOLLOCKS-->"
                + "<script type=\"text/javascript\" src=\"" + jsprefix + "/ready.js\"></script>\n"
                + "<script type=\"text/javascript\">\n"
                + " $.ajaxSetup({async:false});\n" +
                "var turkSetAssignmentID = function () { $.get(\"assignmentId\", function(_aid) { \n" +
                "console.log(\"Just pulled assignment Id : \" + _aid); \n" +
                "document.getElementById(\"assignmentId\").value = _aid; \n" +
                "aid = _aid; \n" +
                "}); }; \n"
                + "</script>\n";
    }

    public String getActionForm(Record record) {
        return "";
    }

    public LocalHTML(){}
}
