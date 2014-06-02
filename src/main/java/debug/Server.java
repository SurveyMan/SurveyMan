package debug;

import interstitial.Library;
import org.apache.log4j.Logger;
import system.localhost.server.WebHandler;
import system.localhost.server.WebServer;
import system.localhost.server.WebServerException;
import util.Slurpie;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Map;


public class Server {
    public static final int frontPort = 8001;
    public static final Logger LOGGER = Logger.getLogger("SurveyMan");

    public static void main() throws WebServerException {
        WebServer server = WebServer.start(frontPort, new WebHandler() {
            @Override
            public void handle(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {

                String method = httpRequest.getMethod();
                String httpPath = httpRequest.getPathInfo();
                PrintWriter out = httpResponse.getWriter();


                LOGGER.info("HTTP Request: " + method + " " + httpPath);

                String response = "";
                if ("GET".equals(method)) {
                    if (httpPath.startsWith("static")){
                        String[] stuff = httpPath.split(";");
                        PrintStream oldOut = System.out;
                        System.setOut(new PrintStream(".tmp"));
                        Report.main(new String[]{"--report=static", stuff[1]});
                        response = Slurpie.slurp(".tmp");
                        System.setOut(oldOut);
                    } else if (httpPath.startsWith("dynamic")) {
                        String[] stuff = httpPath.split(";");
                        PrintStream oldOut = System.out;
                        System.setOut(new PrintStream(".tmp"));
                        Report.main(new String[]{"--report=dynamic", stuff[1], stuff[2]});
                        response = Slurpie.slurp(".tmp");
                        System.setOut(oldOut);
                    } else {
                        String path = httpPath.replace("/", Library.fileSep).substring(1);
                        try {
                            response = Slurpie.slurp(path);
                        } catch (IOException e) {
                            httpResponse.sendError(404, "Not Found");
                            LOGGER.warn(e);
                            return;
                        }
                    }
                } else if ("POST".equals(method)) {
                    Map<String,String[]> formParams = (Map<String, String[]>) httpRequest.getParameterMap();
                    IdResponseTuple xml = convertToXML(formParams);

                    synchronized (newXmlResponses) {
                        newXmlResponses.add(xml);
                    }

                    response = Slurpie.slurp("thanks.html");
                } else {
                    httpResponse.sendError(400, "Bad Request");
                    return;
                }

                // send response body
                httpResponse.setStatus(200);
                httpResponse.setContentType("text/html");
                out.println(response);
                out.close();
            }
        });
        serving = true;
    }

}
