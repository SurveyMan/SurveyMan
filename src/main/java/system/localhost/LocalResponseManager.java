package system.localhost;

import csv.CSVLexer;
import org.apache.commons.httpclient.HttpHost;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.dom4j.DocumentException;
import org.json.JSONException;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.xml.sax.SAXException;
import survey.Survey;
import survey.SurveyException;
import survey.SurveyResponse;
import system.Gensym;
import system.Record;
import system.interfaces.ResponseManager;
import system.interfaces.Task;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class LocalResponseManager extends ResponseManager {

    private static final Gensym workerIds = new Gensym("w");

    public List<Server.IdResponseTuple> getNewAnswers() throws IOException, URISyntaxException, ParseException, JSONException {
        String responseBody = getRequest();
        ArrayList<Server.IdResponseTuple> responseTuples = new ArrayList<Server.IdResponseTuple>();
        if (responseBody.equals(""))
            return responseTuples;
        JSONParser parser = new JSONParser();
        JSONArray array = (JSONArray) parser.parse(responseBody);
        for (int i = 0 ; i < array.size() ; i++){
            JSONObject obj = (JSONObject) array.get(i);
            String workerId = (String) obj.get("workerid");
            String xml = (String) obj.get("answer");
            Server.IdResponseTuple tuple = new Server.IdResponseTuple(workerId, CSVLexer.htmlChars2XML(xml));
            responseTuples.add(tuple);
        }
        return responseTuples;
    }

    private String getRequest() throws URISyntaxException, IOException {

        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpHost host = new HttpHost("localhost", Server.backPort, Protocol.getProtocol("http"));
        HttpGet request = new HttpGet(host.toURI().concat("/" + Server.RESPONSES));
        //System.out.println("Executing request: " + request.getRequestLine());
        ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
            public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;
                } else {
                    throw new ClientProtocolException("Unexpected response status: " + status);
                }
            }
        };
        String responseBody = httpclient.execute(request, responseHandler);
        System.out.println(responseBody);
        return responseBody;
    }

    @Override
    public int addResponses(Survey survey, Task task) throws SurveyException {
        int responsesAdded = 0;
        Record r = null;
        try {
            r = ResponseManager.getRecord(survey);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (r==null) return -1;
        List<SurveyResponse> responses = r.responses;
        try {
            List<Server.IdResponseTuple> tuples = getNewAnswers();
            for (Server.IdResponseTuple tupe : tuples) {
                SurveyResponse sr = parseResponse(tupe.id, tupe.xml, survey, r, null);
                responses.add(sr);
                responsesAdded++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        if (responsesAdded>0)
            System.out.println(String.format("%d responses total", responses.size()));
        return responsesAdded;
    }

    @Override
    public Task getTask(String taskid) {
        return null;
    }

    @Override
    public List<Task> listAvailableTasksForRecord(Record r) {
        return Arrays.asList(r.getAllTasks());
    }

    @Override
    public boolean makeTaskUnavailable(Task task) {
        return false;
    }

    @Override
    public boolean makeTaskAvailable(String taskId, Record r) {
        return false;
    }

}
