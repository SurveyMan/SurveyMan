package edu.umass.cs.surveyman.analyses;

import edu.umass.cs.surveyman.SurveyMan;
import edu.umass.cs.surveyman.output.*;
import edu.umass.cs.surveyman.qc.*;
import edu.umass.cs.surveyman.qc.biases.OrderBias;
import edu.umass.cs.surveyman.survey.*;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.apache.commons.lang3.StringUtils;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.prefs.CsvPreference;

import java.io.*;
import java.util.*;

/**
 * Implements all of the Dynamic Analysis logic.
 */
public class DynamicAnalysis {

    /**
     * Concretized IQuestionResponse.
     */
    public static class QuestionResponse implements IQuestionResponse {

        private Question q;
        private List<OptTuple> opts = new ArrayList<>();
        private int indexSeen;

        /**
         * QuestionResponse constructor.
         * @param s The survey this response refers to.
         * @param quid The question id for this response.
         * @param qpos The index of the position at which this questio appeared to the respondent.
         * @throws SurveyException
         */
        public QuestionResponse(
                Survey s,
                String quid,
                int qpos)
                throws SurveyException
        {
            this.q = s.getQuestionById(quid);
            this.indexSeen = qpos;
        }

        @Override
        public Question getQuestion()
        {
            return q;
        }

        @Override
        public List<OptTuple> getOpts()
        {
            return opts;
        }

        @Override
        public int getIndexSeen()
        {
            return indexSeen;
        }

        @Override
        public SurveyDatum getAnswer() throws SurveyException
        {
            if (this.getQuestion().exclusive)
                return this.getOpts().get(0).c;
            else throw new RuntimeException("Cannot call getAnswer() on non-exclusive questions. Try getAnswers() instead.");
        }

        @Override
        public List<SurveyDatum> getAnswers() throws SurveyException
        {
            if (this.getQuestion().exclusive)
                throw new RuntimeException("Cannot call getAnswers() on exclusive questions. Try getAnswer() instead.");
            List<SurveyDatum> answers = new ArrayList<>();
            for (OptTuple optTuple : this.getOpts())
                answers.add(optTuple.c);
            return answers;
        }

        @Override
        public int compareTo(Object o)
        {
            if (o instanceof IQuestionResponse) {
                IQuestionResponse that = (IQuestionResponse) o;
                return this.getQuestion().compareTo(that.getQuestion());
            } else throw new RuntimeException(String.format("Cannot compare classes %s and %s",
                    this.getClass().getName(), o.getClass().getName()));
        }
    }

    /**
     * DynamicSurveyResponse extends SurveyResponse, implementing the interface ISurveyResponseReader, which allows this
     * class to read in responses from a web survey output (i.e., those that edu.umass.cs.runner.Runner produces).
     */
    public static class DynamicSurveyResponse extends SurveyResponse implements ISurveyResponseReader {

        public DynamicSurveyResponse(Survey survey, String srid) {
            super(survey);
            this.setSrid(srid);
        }

        public DynamicSurveyResponse(SurveyResponse surveyResponse) {
            super(surveyResponse.getSurvey(),
                    surveyResponse.getAllResponses(),
                    surveyResponse.getSrid(),
                    surveyResponse.getScore(),
                    surveyResponse.getThreshold(),
                    surveyResponse.getKnownValidityStatus());
        }

        @Override
        public Map<String, IQuestionResponse> resultsAsMap()
        {
            Map<String, IQuestionResponse> retval = new HashMap<>();
            for (IQuestionResponse qr : this.getNonCustomResponses()) {
                retval.put(qr.getQuestion().id, qr);
            }
            return retval;
        }

        @Override
        public List<? extends SurveyResponse> readSurveyResponses(
                Survey s,
                Reader r)
                throws SurveyException
        {
            List<SurveyResponse> responses = new LinkedList<>();
            final CellProcessor[] cellProcessors = s.makeProcessorsForResponse();
            try{
                ICsvMapReader reader = new CsvMapReader(r, CsvPreference.STANDARD_PREFERENCE);
                final String[] header = reader.getHeader(true);
                Map<String, Object> headerMap;
                SurveyResponse sr = null;
                while ((headerMap = reader.read(header, cellProcessors)) != null) {
                // loop through one survey response (i.e. per responseid) at a time
                    if ( sr == null || !sr.getSrid().equals(headerMap.get("responseid"))){
                        if (sr!=null)
                        // add this to the list of responses and create a new one
                            responses.add(sr);
                        sr = new DynamicSurveyResponse(s, (String) headerMap.get("workerid"));
                        sr.setSrid((String) headerMap.get("responseid"));
                    }
                    // fill out the individual question responses
                    IQuestionResponse questionResponse =
                            new QuestionResponse(
                                    s,
                                    (String) headerMap.get("questionid"),
                                    (Integer) headerMap.get("questionpos"));
                    for (IQuestionResponse qr : sr.getNonCustomResponses())
                        if (qr.getQuestion().id.equals(headerMap.get("questionid"))) {
                        // if we already have a QuestionResponse object matching this id, set it
                            questionResponse = qr;
                            break;
                        }
                    SurveyDatum c;
                    if (!Question.customQuestion(questionResponse.getQuestion().id))
                        c = questionResponse.getQuestion().getOptById((String) headerMap.get("optionid"));
                    else c = new StringDatum((String) headerMap.get("optionid"), -1, -1, -1);
                    Integer i = (Integer) headerMap.get("optionpos");
                    questionResponse.getOpts().add(new OptTuple(c,i));
                    sr.getNonCustomResponses().add(questionResponse);
                }
                reader.close();
                return responses;
            } catch (IOException io) {
                io.printStackTrace();
            }
            return null;
        }

        @Override
        public boolean surveyResponseContainsAnswer(
                List<SurveyDatum> variants)
        {
            throw new RuntimeException("Should not be calling surveyResponseContainsAnswer from inside Dynamic Analysis.");
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
                ClassifiedRespondentsStruct classifiedResponses)
        {
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


        public void print(
                OutputStream stream)
        {
            OutputStreamWriter osw = new OutputStreamWriter(stream);
            try {
                String[] toprint = {
                        this.orderBiases.toString(),
                        this.wordingBiases.toString(),
                        this.breakoffByPosition.toString(),
                        this.breakoffByQuestion.toString(),
                        this.classifiedResponses.toString()
                };
                osw.write(StringUtils.join(toprint, "\n"));
                osw.flush();
            } catch (IOException io) {
                io.printStackTrace();
                SurveyMan.LOGGER.warn(io);
            }
        }
    }

    public static Report dynamicAnalysis(
            Survey survey,
            List<DynamicSurveyResponse> responses,
            Classifier classifier,
            boolean smoothing,
            double alpha)
            throws SurveyException {
        QCMetrics qcMetrics = new QCMetrics(survey, smoothing);
        return new Report(
                survey.sourceName,
                survey.sid,
                alpha,
                smoothing,
                OrderBias.calculateOrderBiases(survey, responses, alpha),
                qcMetrics.calculateWordingBiases(responses, alpha),
                qcMetrics.calculateBreakoffByPosition(responses),
                qcMetrics.calculateBreakoffByQuestion(responses),
                qcMetrics.classifyResponses(responses, classifier, alpha)
            );
   }


    /**
     * Parses the responses to survey s contained in the file named filename. Returns a list of SurveyResponses.
     * @param s The survey that the responses being read from filename had answered.
     * @param filename The url string corresponding to the file containing the survey responses.
     * @return A list of SurveyResponses.
     * @throws SurveyException
     */
    public static List<DynamicSurveyResponse> readSurveyResponses(
            Survey s,
            String filename)
            throws SurveyException
    {
        List<DynamicSurveyResponse> responses = null;
        if (new File(filename).isFile()) {
            try {
                responses = readSurveyResponses(s, new FileReader(filename));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else if (new File(filename).isDirectory()) {
            responses = new ArrayList<>();
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

    /**
     * Parses the responses to survey s contained in r. Returns a list of SurveyResponses.
     * @param s The survey that the responses being read by Reader r had answered.
     * @param r The reader that feeds in the responses to be parsed.
     * @return A list of SurveyResponses.
     * @throws SurveyException
     */
    public static List<DynamicSurveyResponse> readSurveyResponses(
            Survey s,
            Reader r)
            throws SurveyException
    {
        List<DynamicSurveyResponse> responses = new LinkedList<DynamicSurveyResponse>();
        final CellProcessor[] cellProcessors = s.makeProcessorsForResponse();
        try{
            ICsvMapReader reader = new CsvMapReader(r, CsvPreference.STANDARD_PREFERENCE);
            final String[] header = reader.getHeader(true);
            Map<String, Object> headerMap;
            DynamicSurveyResponse sr = null;
            while ((headerMap = reader.read(header, cellProcessors)) != null) {
                // loop through one survey response (i.e. per responseid) at a time
                if ( sr == null || !sr.getSrid().equals(headerMap.get("responseid"))){
                    if (sr!=null)
                    // add this to the list of responses and create a new one
                        responses.add(sr);
                    sr = new DynamicSurveyResponse(s, (String) headerMap.get("workerid"));
                    sr.setSrid((String) headerMap.get("responseid"));
                }
                // fill out the individual question responses
                IQuestionResponse questionResponse = new QuestionResponse(
                        s,
                        (String) headerMap.get("questionid"),
                        (Integer) headerMap.get("questionpos"));
                for (IQuestionResponse qr : sr.getNonCustomResponses())
                    if (qr.getQuestion().id.equals((String) headerMap.get("questionid"))) {
                    // if we already have a QuestionResponse object matching this id, set it
                        questionResponse = qr;
                        break;
                    }
                SurveyDatum c;
                if (!Question.customQuestion(questionResponse.getQuestion().id)) {
                    String id = (String) headerMap.get("optionid");
                    if (id.startsWith("comp")) {
                        SurveyMan.LOGGER.warn(String.format("Deprecated identifier convention: %s", id));
                        id = id.replace("comp", "data");
                    }
                    c = questionResponse.getQuestion().getOptById(id);
                } else c = new StringDatum((String) headerMap.get("optionid"), -1, -1, -1);
                Integer i = (Integer) headerMap.get("optionpos");
                questionResponse.getOpts().add(new OptTuple(c,i));
                sr.addResponse(questionResponse);
            }
            reader.close();
            return responses;
        } catch (IOException io) {
            io.printStackTrace();
        }
        return null;
    }

}