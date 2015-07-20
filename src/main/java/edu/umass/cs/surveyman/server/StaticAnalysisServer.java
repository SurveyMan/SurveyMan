package edu.umass.cs.surveyman.server;

import edu.umass.cs.surveyman.analyses.StaticAnalysis;
import edu.umass.cs.surveyman.input.csv.CSVLexer;
import edu.umass.cs.surveyman.input.csv.CSVParser;
import edu.umass.cs.surveyman.input.json.JSONParser;
import edu.umass.cs.surveyman.qc.Classifier;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.Slurpie;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jfoley.
 */
public class StaticAnalysisServer implements AutoCloseable {
  // These are customizable in the request, but should have sane defaults here:
  public static Classifier DEFAULT_CLASSIFIER = Classifier.STACKED;
  public static int DEFAULT_N = 100;
  public static double DEFAULT_GRANULARITY = 0.1;
  public static double DEFAULT_ALPHA = 0.1;

  private final Server server;

  public StaticAnalysisServer(int port) {
    this.server = new Server(port);
    this.server.setHandler(new AbstractHandler() {
      @Override
      public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        baseRequest.setHandled(true);
        try {
          analyzeRequest(request, response);
        } catch (SurveyException e) {
          e.printStackTrace();
          response.sendError(400, "SurveyException: "+e.getMessage());
        } catch (Exception e) {
          e.printStackTrace();
          System.out.println("HELLO?");
          // Catch-all for exceptions, send a "Internal-Error" in response to this request.
          response.sendError(501, e.getMessage());
        }
      }
    });
  }

  public int getPort() {
    return server.getConnectors()[0].getPort();
  }

  public static String getHostName() {
    try {
      return java.net.InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      e.printStackTrace();
      return "localhost";
    }
  }

  public String getURL() {
    return String.format("http://%s:%d", getHostName(), getPort());
  }

  public static void main(String[] args) throws Exception {
    int port = args.length > 0 ? Integer.parseInt(args[0]) : 1234;
    StaticAnalysisServer sas = new StaticAnalysisServer(port);
    sas.startServer();
    System.out.println("StaticAnalysisServer listening on: " + sas.getURL());
    sas.joinServer(); // block this main thread.
  }

  public void startServer() throws Exception {
    server.start();
  }

  private void joinServer() throws Exception {
    server.join();
  }


  public static void analyzeRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, SurveyException {
    String path = request.getPathInfo();
    if(!path.equals("/analyze")) {
      response.sendError(400, "No method for path.");
      return;
    }
    String method = request.getMethod();
    switch(method) {
      case "POST": break;
      default:
        response.sendError(400, "Bad request method.");
        return;
    }

    String contentType = request.getContentType();
    int semicolon = contentType.indexOf(';');
    if(semicolon >= 0) {
      contentType = contentType.substring(0, semicolon);
    }

    Survey survey;
    switch (contentType) {
      case "application/json":
      case "text/json":
        survey = parseJSON(request);
        break;
      case "text/csv":
        survey = parseCSV(request);
        break;
      default:
        response.sendError(400, "Bad content-type of request.");
        return;
    }

    Map<String,String> qp = parseQueryParams(request);

    Classifier classifier = Classifier.valueOf(getOrElse(qp, "classifier", DEFAULT_CLASSIFIER.name()));
    int n = Integer.parseInt(getOrElse(qp, "n", Integer.toString(DEFAULT_N)));
    double granularity = Double.parseDouble(getOrElse(qp, "granularity", Double.toString(DEFAULT_GRANULARITY)));
    double alpha = Double.parseDouble(getOrElse(qp, "alpha", Double.toString(DEFAULT_ALPHA)));

    StaticAnalysis.Report report = StaticAnalysis.staticAnalysis(survey, classifier, n, granularity, alpha);

    try (OutputStream out = response.getOutputStream()) {
      report.print(out);
      response.setStatus(200);
      response.setContentType("text/plain/");
    }
  }

  public static <K,V> V getOrElse(Map<K,V> map, K key, V fallback) {
    V value = map.get(key);
    if(value == null) return fallback;
    return value;
  }

  public static Map<String, String> parseQueryParams(HttpServletRequest request) {
    Map<String,String> results = new HashMap<>();
    Enumeration pnames = request.getParameterNames();
    while(pnames.hasMoreElements()) {
      String name = (String) pnames.nextElement();
      results.put(name, request.getParameter(name));
    }
    return results;
  }

  public static Survey parseCSV(HttpServletRequest request) throws IOException, SurveyException {
    try (BufferedReader reader = request.getReader()) {
      CSVParser parser = new CSVParser(new CSVLexer(reader));
      return parser.parse();
    } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  public static Survey parseJSON(HttpServletRequest request) throws IOException, SurveyException {
    String json;
    try (BufferedReader reader = request.getReader()) {
      json = Slurpie.slurp(reader, Integer.MAX_VALUE);
    }
    JSONParser parser = new JSONParser(json);
    return parser.parse();
  }

  public void close() throws Exception {
    server.stop();
  }

}
