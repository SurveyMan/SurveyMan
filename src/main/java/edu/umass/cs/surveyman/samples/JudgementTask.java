package edu.umass.cs.surveyman.samples;

import edu.umass.cs.surveyman.analyses.AbstractRule;
import edu.umass.cs.surveyman.analyses.StaticAnalysis;
import edu.umass.cs.surveyman.survey.HTMLDatum;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

/**
 * Created by etosch on 3/5/15.
 */
public class JudgementTask {

    public static Survey makeSurvey()
            throws SurveyException
    {
        Survey survey = new Survey();
        String instructions = "<p>Please take a moment to look at the following webpage:</p>";
        String relevance = "<p>Which of the following links is relevant?</p>";
        String[]  links = {"../src/main/resources/debugger/SurveyMan.html", "http://www.weather.com"};
        Question q1 = new Question(new HTMLDatum(String.format("%s<iframe src='%s'></iframe>%s", instructions, links[0], relevance)));
        q1.addOption("<a href='http://surveyman.github.io/SurveyMan/target/site/apidocs/index.html'>SurveyMan API</a>");
        q1.addOption("<a href='http://google.com'>Google Search</a>");
        Question q2 = new Question(new HTMLDatum(String.format("%s<iframe src='%s'></iframe>%s", instructions, links[1], relevance)));
        q2.addOption("<a href='http://weatherunderground.com/'>Weather Underground</a>");
        q2.addOption("<a href='http://twitter.com/'>Twitter</a>");
        survey.addQuestions(q1, q2);
        AbstractRule.getDefaultRules();
        StaticAnalysis.wellFormednessChecks(survey);
        return survey;
    }

}
