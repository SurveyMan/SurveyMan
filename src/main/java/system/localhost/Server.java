package system.localhost;

import csv.CSVLexer;
import org.apache.log4j.Logger;
import system.Gensym;
import system.Library;
import system.Slurpie;
import java.io.*;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Server {

    public static final String RESPONSES = "responses";
    public static final Logger LOGGER = Logger.getLogger(Server.class);

    public static class IdResponseTuple {
        public String id, xml;
        public IdResponseTuple(String id, String xml) {
            this.id = id; this.xml = xml;
        }
        protected String jsonize() {
            return String.format("{\"workerid\" : \"%s\", \"answer\" : \"%s\"}", id, CSVLexer.xmlChars2HTML(xml));
        }
    }

    static class MyThread extends Thread {
        final ServerSocket socket;
        public MyThread(ServerSocket socket) {
            this.socket = socket;
        }
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

    /** from http://library.sourcerabbit.com/v/?id=19 **/
    // need to validate against a backend

    public static Gensym gensym = new Gensym("a");
    public static volatile int frontPort = 8000;
    public static volatile int backPort = 8001;
    public static final int numThreads = 100;
    public static final Executor threadPool = Executors.newFixedThreadPool(numThreads);
    public static boolean serving = false;
    public static List<IdResponseTuple> newXmlResponses = new ArrayList<IdResponseTuple>();
    public static List<IdResponseTuple> oldXmlResponses = new ArrayList<IdResponseTuple>();
    public static int requests = 0;
    public static Thread frontEnd, backEnd;

    public static void startServe() throws IOException {
        serving = true;
        boolean frontSet = false, backSet = false;
        while (!frontSet) {
            try{
                final ServerSocket frontSocket = new ServerSocket(frontPort);
                frontEnd = new MyThread(frontSocket);
                frontEnd.start();
                frontSet = true;
            } catch (BindException e) {
                LOGGER.warn(e);
                frontPort++;
            }
        }
        while (!backSet) {
            try {
                final ServerSocket backSocket = new ServerSocket(backPort);
                Thread backEnd = new MyThread(backSocket);
                backEnd.start();
                backSet = true;
            } catch (BindException e) {
                LOGGER.warn(e);
                backPort++;
            }
        }
        String msg = String.format("Backend running on port %d; frontend running on port %d", backPort, frontPort);
        LOGGER.info(msg);
        System.out.println(msg);
    }

    public static void endServe() throws InterruptedException {
        serving = false;
        frontEnd.join();
        backEnd.join();
    }

    private static String getJsonizedNewResponses() {
        synchronized (newXmlResponses) {
            Iterator<IdResponseTuple> tupes = newXmlResponses.iterator();
            StringBuilder sb = new StringBuilder();
            if (tupes.hasNext()) {
                IdResponseTuple tupe = tupes.next();
                sb.append(tupe.jsonize());
                tupes.remove();
                oldXmlResponses.add(tupe);
            } else return "";
            while (tupes.hasNext()) {
                IdResponseTuple tupe = tupes.next();
                sb.append(String.format(", %s", tupe.jsonize()));
            }
            for (IdResponseTuple tupe : oldXmlResponses) {
                if (newXmlResponses.contains(tupe))
                    newXmlResponses.remove(tupe);
            }
            return String.format("[%s]", sb.toString());
        }
    }


    private static void handleRequest(Socket s) throws IOException {

        BufferedReader in;
        PrintWriter out;
        String request;

        String webserveraddress = s.getInetAddress().toString();
        LOGGER.info("New Connection:" + webserveraddress);
        InputStream is = s.getInputStream();
        in = new BufferedReader(new InputStreamReader(is));
        request = in.readLine();
        LOGGER.info("--- Client request: " + request);

        String[] pieces = request.split("\\s+");
        String response = "";
        LOGGER.info("cwd: " + new File(".").getCanonicalPath());

        if (pieces[0].equals("GET")) {
            if (pieces[1].endsWith(RESPONSES))
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
            char[] maybeContent = new char[numBytes];
            in.read(maybeContent);
            String stuff = URLDecoder.decode(String.valueOf(maybeContent), "UTF-8");
            IdResponseTuple xml = convertToXML(stuff);
            newXmlResponses.add(xml);
            System.out.println(xml);
        }

        out = new PrintWriter(s.getOutputStream(), true);
        out.println("HTTP/1.0 200");
        out.println("Content-type: text/html");
        out.println("Server-name: LOCALHOST");
        out.println("Content-length: " + response.length());
        out.println("");
        out.println(response);
        out.flush();
        out.close();
        s.close();

        requests++;
    }

    public static IdResponseTuple convertToXML(String response) {
        // while the answer doesn't need to go be converted to XML, this is set up to double as an offline simulator for mturk.
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><QuestionFormAnswers xmlns=\"http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2005-10-01/QuestionFormAnswers.xsd\">");
        String assignmentId = "";
        if (response.equals(""))
            return null;
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
