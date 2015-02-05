package edu.umass.cs.surveyman.analyses;

import edu.umass.cs.surveyman.SurveyMan;
import edu.umass.cs.surveyman.output.*;
import edu.umass.cs.surveyman.qc.*;
import edu.umass.cs.surveyman.survey.Component;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.StringComponent;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.prefs.CsvPreference;

import java.io.*;
import java.util.*;

public class DynamicAnalysis {

    public static class QuestionResponse implements IQuestionResponse {

        private Question q;
        private List<OptTuple> opts = new ArrayList<OptTuple>();
        private int indexSeen;

        public QuestionResponse(Survey s, String quid, int qpos) throws SurveyException {
            this.q = s.getQuestionById(quid);
            this.indexSeen = qpos;
        }

        @Override
        public Question getQuestion() {
            return q;
        }

        @Override
        public List<OptTuple> getOpts() {
            return opts;
        }

        @Override
        public int getIndexSeen() {
            return indexSeen;
        }

    }

    public static class SurveyResponse implements ISurveyResponse {

        private List<IQuestionResponse> responses = new ArrayList<IQuestionResponse>();
        private boolean recorded;
        private String srid;
        private String workerid;

        // constructor without all the Mechanical Turk stuff (just for testing)
        public SurveyResponse(String wID){
            this.workerid = wID;
            this.srid = wID;
        }

        @Override
        public List<IQuestionResponse> getResponses() {
            return responses;
        }

        @Override
        public void setResponses(List<IQuestionResponse> responses) {
            this.responses = responses;
        }

        @Override
        public boolean isRecorded() {
            return recorded;
        }

        @Override
        public void setRecorded(boolean recorded) {
            this.recorded = recorded;
        }

        @Override
        public String getSrid() {
            return srid;
        }

        @Override
        public void setSrid(String srid) {
            this.srid = srid;
        }

        @Override
        public String workerId() {
            return this.workerid;
        }

        @Override
        public Map<String, IQuestionResponse> resultsAsMap() {
            Map<String, IQuestionResponse> retval = new HashMap<String, IQuestionResponse>();
            for (IQuestionResponse qr : responses) {
                retval.put(qr.getQuestion().quid, qr);
            }
            return retval;
        }

        @Override
        public List<ISurveyResponse> readSurveyResponses(Survey s, Reader r) throws SurveyException {
            List<ISurveyResponse> responses = new LinkedList<ISurveyResponse>();
            final CellProcessor[] cellProcessors = s.makeProcessorsForResponse();
            try{
                ICsvMapReader reader = new CsvMapReader(r, CsvPreference.STANDARD_PREFERENCE);
                final String[] header = reader.getHeader(true);
                Map<String, Object> headerMap;
                ISurveyResponse sr = null;
                while ((headerMap = reader.read(header, cellProcessors)) != null) {
// loop through one survey response (i.e. per responseid) at a time
                    if ( sr == null || !sr.getSrid().equals(headerMap.get("responseid"))){
                        if (sr!=null)
// add this to the list of responses and create a new one
                            responses.add(sr);
                        sr = new SurveyResponse((String) headerMap.get("workerid"));
                        sr.setSrid((String) headerMap.get("responseid"));
                    }
// fill out the individual question responses
                    IQuestionResponse questionResponse =
                            new QuestionResponse(
                                    s,
                                    (String) headerMap.get("questionid"),
                                    (Integer) headerMap.get("questionpos"));
                    for (IQuestionResponse qr : sr.getResponses())
                        if (qr.getQuestion().quid.equals((String) headerMap.get("questionid"))) {
// if we already have a QuestionResponse object matching this id, set it
                            questionResponse = qr;
                            break;
                        }
                    Component c;
                    if (!Question.customQuestion(questionResponse.getQuestion().quid))
                        c = questionResponse.getQuestion().getOptById((String) headerMap.get("optionid"));
                    else c = new StringComponent((String) headerMap.get("optionid"), -1, -1);
                    Integer i = (Integer) headerMap.get("optionpos");
                    questionResponse.getOpts().add(new OptTuple(c,i));
                    sr.getResponses().add(questionResponse);
                }
                reader.close();
                return responses;
            } catch (IOException io) {
                io.printStackTrace();
            }
            return null;
        }


        @Override
        public void setScore(double score) {

        }

        @Override
        public double getScore() {
            return 0;
        }

        @Override
        public void setThreshold(double pval) {

        }

        @Override
        public double getThreshold() {
            return 0;
        }

        @Override
        public boolean surveyResponseContainsAnswer(List<Component> variants) {
            return false;
        }

        @Override
        public boolean hasResponseForQuestion(Question q) {
            return false;
        }

        @Override
        public IQuestionResponse getResponseForQuestion(Question q) {
            return null;
        }

        @Override
        public KnownValidityStatus getKnownValidityStatus() {
            return null;
        }

        @Override
        public void setKnownValidityStatus(KnownValidityStatus validityStatus) {

        }
    }

   public static class Report {

        public final String surveyName;
        public final String sid;
        public final double alpha;
        public final boolean smoothing;
        public final OrderBiasStruct orderBiases;
        public final WordingBiasStruct wordingBiases;
        public final BreakoffByQuestion breakoffByQuestion;
        public final BreakoffByPosition breakoffByPosition;
        public final ClassifiedRespondentsStruct classifiedResponses;

        public Report(
                String surveyName,
                String sid,
                double alpha,
                boolean smoothing,
                OrderBiasStruct orderBiases,
                WordingBiasStruct wordingBiases,
                BreakoffByPosition breakoffByPosition,
                BreakoffByQuestion breakoffByQuestion,
                ClassifiedRespondentsStruct classifiedResponses){
            this.surveyName = surveyName;
            this.sid = sid;
            this.alpha = alpha;
            this.smoothing = smoothing;
            this.orderBiases = orderBiases;
            this.wordingBiases = wordingBiases;
            this.breakoffByPosition = breakoffByPosition;
            this.breakoffByQuestion = breakoffByQuestion;
            this.classifiedResponses = classifiedResponses;
        }


        public void print(OutputStream stream){
            OutputStreamWriter osw = new OutputStreamWriter(stream);
            try {
                osw.write(String.format(
                        "%s\n%s\n%s\n%s\n",
                        this.orderBiases.toString(),
                        this.wordingBiases.toString(),
                        this.breakoffByPosition.toString(),
                        this.breakoffByQuestion.toString()));
                osw.write("asdf");
                osw.write(this.classifiedResponses.toString());
                osw.flush();
            } catch (IOException io) {
                io.printStackTrace();
                SurveyMan.LOGGER.warn(io);
            }
        }
    }

   public static Report dynamicAnalysis(
            Survey survey,
            List<ISurveyResponse> responses,
            Classifier classifier,
            boolean smoothing,
            double alpha)
            throws SurveyException {
        return new Report(
                survey.sourceName,
                survey.sid,
                alpha,
                smoothing,
                QCMetrics.calculateOrderBiases(survey, responses, alpha),
                QCMetrics.calculateWordingBiases(survey, responses, alpha),
                QCMetrics.calculateBreakoffByPosition(survey, responses),
                QCMetrics.calculateBreakoffByQuestion(survey, responses),
                QCMetrics.classifyResponses(survey, responses, classifier, smoothing, alpha)
                );
   }


   public static List<ISurveyResponse> readSurveyResponses(Survey s, String filename) throws SurveyException {
        List<ISurveyResponse> responses = null;
        if (new File(filename).isFile()) {
            try {
                responses = readSurveyResponses(s, new FileReader(filename));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else if (new File(filename).isDirectory()) {
            responses = new ArrayList<ISurveyResponse>();
            for (File f : new File(filename).listFiles()) {
                try {
                    responses.addAll(readSurveyResponses(s, new FileReader(f)));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } else throw new RuntimeException("Unknown file or directory: "+filename);
        return responses;
   }

    public static List<ISurveyResponse> readSurveyResponses(Survey s, Reader r) throws SurveyException {
        List<ISurveyResponse> responses = new LinkedList<ISurveyResponse>();
        final CellProcessor[] cellProcessors = s.makeProcessorsForResponse();
        try{
            ICsvMapReader reader = new CsvMapReader(r, CsvPreference.STANDARD_PREFERENCE);
            final String[] header = reader.getHeader(true);
            Map<String, Object> headerMap;
            ISurveyResponse sr = null;
            while ((headerMap = reader.read(header, cellProcessors)) != null) {
                // loop through one survey response (i.e. per responseid) at a time
                if ( sr == null || !sr.getSrid().equals(headerMap.get("responseid"))){
                    if (sr!=null)
                    // add this to the list of responses and create a new one
                        responses.add(sr);
                    sr = new SurveyResponse((String) headerMap.get("workerid"));
                    sr.setSrid((String) headerMap.get("responseid"));
                }
                // fill out the individual question responses
                IQuestionResponse questionResponse = new QuestionResponse(
                        s,
                        (String) headerMap.get("questionid"),
                        (Integer) headerMap.get("questionpos"));
                for (IQuestionResponse qr : sr.getResponses())
                    if (qr.getQuestion().quid.equals((String) headerMap.get("questionid"))) {
                    // if we already have a QuestionResponse object matching this id, set it
                        questionResponse = qr;
                        break;
                    }
                Component c;
                if (!Question.customQuestion(questionResponse.getQuestion().quid))
                    c = questionResponse.getQuestion().getOptById((String) headerMap.get("optionid"));
                else c = new StringComponent((String) headerMap.get("optionid"), -1, -1);
                Integer i = (Integer) headerMap.get("optionpos");
                questionResponse.getOpts().add(new OptTuple(c,i));
                sr.getResponses().add(questionResponse);
            }
            reader.close();
            return responses;
        } catch (IOException io) {
            io.printStackTrace();
        }
        return null;
    }


}