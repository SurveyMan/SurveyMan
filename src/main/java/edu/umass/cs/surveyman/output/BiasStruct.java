package edu.umass.cs.surveyman.output;

import edu.umass.cs.surveyman.survey.Block;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.SurveyObj;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.Jsonable;
import edu.umass.cs.surveyman.utils.jsonify.Jsonify;
import edu.umass.cs.surveyman.utils.Tabularable;

import java.util.HashMap;
import java.util.Map;

public abstract class BiasStruct implements Jsonable, Tabularable {

    public static <K extends SurveyObj, V extends Jsonable> java.lang.String jsonize(Map<K, V> map) throws SurveyException {
        Map<java.lang.String, Jsonable> m = new HashMap<>();
        for (Map.Entry<K, V> e : map.entrySet()) {
            K key = e.getKey();
            V val = e.getValue();
            m.put(key.getId(), val);
        }
        return Jsonify.jsonify(m);
    }

    protected static class InnerCorrelationStruct extends HashMap<Question, CorrelationStruct> implements Jsonable {

        @Override
        public java.lang.String jsonize() throws SurveyException
        {
            return BiasStruct.jsonize(this);
        }
    }

    protected static class QuestionCorrelationStruct extends HashMap<Question, InnerCorrelationStruct> implements Jsonable {

        @Override
        public java.lang.String jsonize() throws SurveyException
        {
            return BiasStruct.jsonize(this);
        }

        public static void populateStruct(Survey survey, OrderBiasStruct that) {
            for (int i = 0; i < survey.questions.size() - 1; i++) {
                Question q1 = survey.questions.get(i);
                that.biases.put(q1, new InnerCorrelationStruct());
                for (int j = i + 1; j < survey.questions.size(); j++) {
                    Question q2 = survey.questions.get(j);
                    that.biases.get(q1).put(q2, new CorrelationStruct());
                }
            }
        }

    }

    protected static class BlockCorrelationStruct extends HashMap<Block, QuestionCorrelationStruct> implements Jsonable {

        @Override
        public java.lang.String jsonize() throws SurveyException
        {
            return BiasStruct.jsonize(this);
        }

        public static void populateStruct(Survey survey, WordingBiasStruct that) {
            for (Block b : survey.getAllBlocks()) {
                if (b.getBranchParadigm().equals(Block.BranchParadigm.ALL)) {
                    QuestionCorrelationStruct outermap = new QuestionCorrelationStruct();
                    for (Question q1 : b.questions) {
                        InnerCorrelationStruct innermap = new InnerCorrelationStruct();
                        for (Question q2 : b.questions)
                            innermap.put(q2, new CorrelationStruct());
                        outermap.put(q1, innermap);
                    }
                    that.biases.put(b, outermap);
                }
            }
        }
    }


}
