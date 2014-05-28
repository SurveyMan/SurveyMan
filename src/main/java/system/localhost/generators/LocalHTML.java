package system.localhost.generators;

import interstitial.IHTML;
import system.localhost.LocalLibrary;
import system.localhost.Server;
import interstitial.Record;

import java.io.File;
import java.io.IOException;

/**
 * Created by etosch on 2/13/14.
 */
public class LocalHTML implements IHTML {

    public static final int port = Server.frontPort;
    public static final String prefix = "http://localhost:" + port;
    private static final String jsprefix = prefix + "/" + LocalLibrary.jshome;
    private static final String node_mod = "/lib/node_modules";
    private static final String jquery = "/jquery/dist/jquery.min.js";
    private static final String underscore = "/underscore/underscore-min.js";
    private static final String seedrandom = "/seedrandom/seedrandom.min.js";
    private static final String randomize = "/randomize.js";
    private static final String ready = "/ready.js";


    public String getHTMLString() {
        if (runningFromSource())
            return getHTMLStringSource();
        else return getHTMLStringJar();
    }

    private boolean runningFromSource() {
        File f1 = new File(jsprefix);
        File f2 = new File(node_mod);
        return f1.exists() && f2.exists();
    }

    private String getHTMLStringJar() {
        String formatStr = "<script type=\"text/javascript\" src=\"%s\"></script>\n";
        return String.format(formatStr, node_mod +jquery)
                + String.format(formatStr, node_mod + underscore)
                + String.format(formatStr, node_mod + seedrandom)
                + String.format(formatStr, randomize)
                + String.format(formatStr, ready)
                + getSetAssignmentId();
    }

    private String getHTMLStringSource() {
        return  "<script type=\"text/javascript\" src=\""+jsprefix + node_mod + jquery+"\"></script>\n"
                + "<script type=\"text/javascript\" src=\""+jsprefix + node_mod + underscore+"\"></script>\n"
                + "<script type=\"text/javascript\" src=\""+jsprefix + node_mod + seedrandom+"\"></script>\n"
                + "<script type=\"text/javascript\" src=\"" + jsprefix + randomize +"\"></script>\n"
                + "<script type=\"text/javascript\" src=\"" + jsprefix + ready + "\"></script>\n"
                + getSetAssignmentId();
    }

    private String getSetAssignmentId(){
        return "<script type=\"text/javascript\">\n"+
                " $.ajaxSetup({async:false});\n" +
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
