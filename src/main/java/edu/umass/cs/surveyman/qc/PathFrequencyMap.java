package edu.umass.cs.surveyman.qc;

import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.survey.Block;

import java.util.*;

public class PathFrequencyMap extends HashMap<SurveyPath, ArrayList<SurveyResponse>>{

    /**
     * Returns the counts for each path; see @etosch's blog post on the calculation.
     * @param paths The list of list of blocks through the survey; can be obtained with getPaths or getDag
     * @param responses The list of actual or simulated responses to the survey
     * @return A map from path to the frequency the path is observed.
     */
    public static PathFrequencyMap makeFrequenciesForPaths(List<SurveyPath> paths, List<? extends SurveyResponse> responses) {
        PathFrequencyMap retval = new PathFrequencyMap();
        // initialize the map
        for (SurveyPath path : paths)
            retval.put(path, new ArrayList<SurveyResponse>());
        for (SurveyResponse r : responses) {
            Set<Block> pathTraversed = SurveyPath.getPath(r);
            boolean pathFound = false;
            for (SurveyPath path : retval.keySet()) {
                if (path.containsAll(pathTraversed)){
                    retval.get(path).add(r);
                    pathFound = true;
                    break;
                }
            }
            assert pathFound : "Path survey respondent took does not match any known paths through the survey.";
        }
        return retval;
    }
}
