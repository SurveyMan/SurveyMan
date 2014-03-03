package system.localhost;

import com.sun.swing.internal.plaf.synth.resources.synth_zh_HK;
import system.Gensym;
import system.Library;
import system.Slurpie;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Server {

    public static class IdResponseTuple {
        public String id, xml;
        public IdResponseTuple(String id, String xml) {
            this.id = id; this.xml = xml;
        }
        public String jsonize() {
            return String.format("{\"workerid\" : \"%s\", \"answer\" : \"%s\"}", id, xml);
        }
    }

    /** from http://library.sourcerabbit.com/v/?id=19 **/
    // need to validate against a backend

    public static Gensym gensym = new Gensym("a");
    public static final int port = 8000;
    public static final int numThreads = 100;
    public static final Executor threadPool = Executors.newFixedThreadPool(numThreads);
    public static boolean serving = false;
    public static List<IdResponseTuple> newXmlResponses = new ArrayList<IdResponseTuple>();
    public static List<IdResponseTuple> oldXmlResponses = new ArrayList<IdResponseTuple>();
    public static int requests = 0;

    public static Thread startServe() throws IOException {
        if (serving) return null;
        serving = true;
        final ServerSocket socket = new ServerSocket(port);
        Thread t = new Thread() {
            public void run() {
                while (serving) {
                    final Socket connection;
                    try {
                        connection = socket.accept();
                        Runnable task = new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    handleRequest(connection);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        };
                        threadPool.execute(task);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        t.start();
        return t;
    }

    public static void endServe(Thread t) throws InterruptedException {
        serving = false;
        t.join();
    }

    private static String getJsonizedNewResponses() {
        synchronized (newXmlResponses) {
            Iterator<IdResponseTuple> tupes = newXmlResponses.iterator();
            StringBuilder sb = new StringBuilder();
            if (tupes.hasNext()) {
                IdResponseTuple tupe = tupes.next();
                sb.append(tupe.jsonize());
                newXmlResponses.remove(tupe);
                oldXmlResponses.add(tupe);
            } else return "";
            while (tupes.hasNext()) {
                IdResponseTuple tupe = tupes.next();
                sb.append(String.format(", %s", tupe.jsonize()));
            }
            return String.format("[%s]", sb.toString());
        }
    }


    private static void handleRequest(Socket s) throws IOException {

        BufferedReader in;
        PrintWriter frontEnd, backEnd;
        String request;

        String webserveraddress = s.getInetAddress().toString();
        System.out.println("New Connection:" + webserveraddress);
        InputStream is = s.getInputStream();
        in = new BufferedReader(new InputStreamReader(is));
        request = in.readLine();
        System.out.println("--- Client request: " + request);

        String[] pieces = request.split("\\s+");
        String response = "";
        System.out.println("cwd: " + new File(".").getCanonicalPath());

        if (pieces[0].equals("GET")) {
            if (pieces[1].equals("responses"))
                response = getJsonizedNewResponses();
            else if (pieces[1].endsWith("assignmentId"))
                response = gensym.next();
            else {
                String path = pieces[1].replace("/", Library.fileSep);
                response = Slurpie.slurp("."+path);
            }
        } else if (pieces[0].equals("POST")) {
            int numBytes = 0;
            while (in.ready()) {
                // read in headers
                String header = in.readLine();
                System.out.println("Header:\t"+header);
                if (header.equals(""))
                    break;
                if (header.startsWith("Content-Length"))
                    numBytes = Integer.parseInt(header.split(":")[1].trim());
            }
            // try reading these bytes
            System.out.println("Done with headers");
            char[] maybeContent = new char[numBytes];
            in.read(maybeContent);
            String stuff = URLDecoder.decode(String.valueOf(maybeContent), "UTF-8");
            IdResponseTuple xml = convertToXML(stuff);
            newXmlResponses.add(xml);
            System.out.println(xml);
        }

        backEnd = new PrintWriter(s.getOutputStream(), true);
        backEnd.println("HTTP/1.0 200");
        backEnd.println("Content-type: text/html");
        backEnd.println("Server-name: myserver");
        backEnd.println("Content-length: " + response.length());
        backEnd.println("");
        backEnd.println(response);
        backEnd.flush();
        backEnd.close();
        s.close();

        requests++;
    }

    public static IdResponseTuple convertToXML(String response) {
        // while the answer doesn't need to go be converted to XML, this is set up to double as an offline simulator for mturk.
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><QuestionFormAnswers xmlns=\"http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2005-10-01/QuestionFormAnswers.xsd\">");
        String assignmentId = "";
        for (String answer : response.split("&")) {
            String[] pairs = answer.split("=");
            xml.append(String.format("<Answer><QuestionIdentifier>%s</QuestionIdentifier><FreeText>%s</FreeText></Answer>", pairs[0], pairs[1]));
            if (pairs[0].equals("assignmentId"))
                assignmentId = pairs[1];
        }
        xml.append("</QuestionFormAnswers>");
        return new IdResponseTuple(assignmentId, xml.toString());
    }
}
