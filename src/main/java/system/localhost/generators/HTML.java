package system.localhost.generators;

import system.localhost.LocalLibrary;
import system.localhost.Server;
import system.Record;

/**
 * Created by etosch on 2/13/14.
 */
public class HTML implements system.interfaces.HTML {

    public static final int port = Server.frontPort;
    public static final String prefix = "http://localhost:" + port;

    public String getHTMLString() {
        String jsprefix = prefix + "/" + LocalLibrary.jshome;
        return "<script type=\"text/javascript\" src=\"" + jsprefix + "/lib/node_modules/jquery/dist/jquery.js\"></script>"
                + "<script type=\"text/javascript\" src=\"" + jsprefix + "/lib/node_modules/underscore/underscore.js\"></script>"
                + "<script type=\"text/javascript\" src=\"" + jsprefix + "/lib/seedrandom/seedrandom.js\"></script>"
                + "<script type=\"text/javascript\" src=\"" + jsprefix + "/randomize.js\"></script>"
                + "<script type=\"text/javascript\" src=\"" + jsprefix + "/ready.js\"></script>"
                + "<script type=\"text/javascript\">"
                + " $.ajaxSetup({async:false}); " +
                "var turkSetAssignmentID = function () { $.get(\"assignmentId\", function(_aid) { " +
                "console.log(\"Just pulled assignment Id : \" + _aid); " +
                "document.getElementById(\"assignmentId\").value = _aid; " +
                "aid = _aid; " +
                "}); }; "
                + "</script>"
                ;
    }

    public String getActionForm(Record record) {
        return "";
    }
}
