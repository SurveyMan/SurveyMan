package edu.umass.cs.surveyman.qc.classifiers;

/**
 * Created by etosch on 11/23/15.
 */
public class StackedClassifier extends AbstractClassifier {
} else if (classifier.equals(Classifier.STACKED)) {
        lpoClassification(responses, 0.5);
        clusterResponses(responses);
        for (SurveyResponse sr : responses)
        classificationStructs.add(new ClassificationStruct(sr, Classifier.STACKED));
        return classificationStructs;
        }

        }
