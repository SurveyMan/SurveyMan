package qc;

import survey.Question;
import survey.SurveyResponse;
import java.util.List;

public class Report {

    static class QuestionPair {
        public Question left, right;
        public QuestionPair(Question q1, Question q2) {
            this.left = q1;
            this.right = q2;
        }
    }

    public List<SurveyResponse> validResponses;
    public List<SurveyResponse> botResponses;
    public List<Question> breakoffQuestions;
    public List<QuestionPair> orderBias;
    public List<Question> variants;
    public double staticMaxEntropy;

    public Report(QC qc) {
        this.validResponses = qc.validResponses;
        this.botResponses = qc.botResponses;
        this.staticMaxEntropy = QCMetrics.getMaxPossibleEntropy(qc.survey);
    }

    public String toString() {
        StringBuilder report = new StringBuilder();
        report.append(String.format("Max possible bits to represent this survey: %f", staticMaxEntropy));
        // report final bot classifications
        // get the number of bots classified
        report.append(String.format("Total number of classified bots : %d\n", botResponses.size()));
        report.append(String.format("Bot classification threshold: %f\n"));
        // add the bot ids and something about the scores
        StringBuilder botids = new StringBuilder();
        for (SurveyResponse sr : validResponses) {
        }
        // report on order bias
        // report on variant bias
        // report on breakoff
        return report.toString();
    }
}
