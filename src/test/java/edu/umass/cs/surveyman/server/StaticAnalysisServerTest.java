package edu.umass.cs.surveyman.server;

import edu.umass.cs.surveyman.utils.Slurpie;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.junit.Assert.assertEquals;

/**
 * @author jfoley.
 */
public class StaticAnalysisServerTest {
  public static final int TESTING_PORT = 1234;

  public static CloseableHttpResponse postJSON(String json, String url, String path) throws IOException {
    try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
      HttpPost post = new HttpPost(url+path);
      post.setHeader("Content-Type", "application/json");
      post.setEntity(new StringEntity(json));
      return client.execute(post);
    }
  }

  // Ignored for now because I don't really understand analysis parameters
  @Ignore
  @Test
  public void test() throws Exception {
    try (StaticAnalysisServer sas = new StaticAnalysisServer(TESTING_PORT)) {
      String url = sas.getURL();
      sas.startServer();

      String json = Slurpie.slurp("ex0.json");
      try (CloseableHttpResponse response = postJSON(json, url, "/analyze")) {
        StatusLine statusLine = response.getStatusLine();
        System.out.println(statusLine);
        assertEquals(200, statusLine.getStatusCode());

        String analysis;
        try (BufferedReader resp = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
          analysis = Slurpie.slurp(resp, Integer.MAX_VALUE);
        }

        System.out.println(analysis);
      }
    }
  }
}