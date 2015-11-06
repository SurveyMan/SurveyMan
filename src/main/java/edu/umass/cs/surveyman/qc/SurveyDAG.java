package edu.umass.cs.surveyman.qc;

import edu.umass.cs.surveyman.SurveyMan;
import edu.umass.cs.surveyman.survey.Block;
import edu.umass.cs.surveyman.survey.Survey;

import java.util.*;

public class SurveyDAG extends ArrayList<SurveyPath> {

    public Survey survey;

    private SurveyDAG() {
    }

    private static HashMap<Survey, List<SurveyPath>> cache = new HashMap<>();

    public SurveyDAG(Survey survey, SurveyPath ...paths) {
        this();
        this.survey = survey;
        Collections.addAll(this, paths);
        cache.put(survey, Arrays.asList(paths));
    }


    /**
     * Returns the DAG for the provided survey.
     * @param survey The survey whose DAG we want.
     * @return A new DAG object.
     */
    public static SurveyDAG getDag(Survey survey) {
        if (cache.containsKey(survey)) {
            SurveyDAG retval = new SurveyDAG();
            retval.survey = survey;
            retval.addAll(cache.get(survey));
            return retval;
        } else {
            SurveyDAG surveyDAG = getDag(survey.topLevelBlocks);
            surveyDAG.survey = survey;
            cache.put(survey, surveyDAG);
            return surveyDAG;
        }
    }

    /**
     * Takes in a list of Blocks; returns a list of lists of Blocks representing all possible paths through the survey.
     * See @etosch's blog post for more detail.
     * @param blockList A list of blocks we would like to traverse.
     * @return A list of lists of blocks, giving all possible traversals through the original input.
     */
    private static SurveyDAG getDag(List<Block> blockList) {
        blockList = Block.getSorted(blockList);
        if (blockList.isEmpty()) {
            // return a singleton list of the empty list
            SurveyDAG newSingletonList = new SurveyDAG();
            newSingletonList.add(new SurveyPath());
            return newSingletonList;
        } else {
            Block thisBlock = blockList.get(0);
            if (thisBlock.hasBranchQuestion()) {
                Set<Block> dests = thisBlock.getBranchDestinations();
                SurveyDAG blists = new SurveyDAG();
                for (Block b : dests) {
                    // for each destination, find the sublist of the blocklist starting with the destination
                    int index = b == null ? 1 : blockList.indexOf(b);
                    if (index > -1) {
                        SurveyDAG dags = getDag(blockList.subList(index, blockList.size()));
                        for (SurveyPath dag : dags) {
                            dag.add(thisBlock);
                        }
                        blists.addAll(dags);
                    }
                }
                return blists;
            } else {
                SurveyDAG subDag = getDag(blockList.subList(1, blockList.size()));
                for (SurveyPath blist : subDag) {
                    blist.add(thisBlock);
                }
                return subDag;
            }
        }
    }

    /**
     * Returns paths through **blocks** in the survey. Top level randomized blocks are all listed last
     * @param s The survey whose paths we want to enumerate
     * @return A List of all paths through the survey. A path is represented by a List. There may be duplicate paths,
     * so if you need distinct paths, you will need to filter for uniqueness.
     */
    public static List<SurveyPath> getPaths(Survey s) {
        if (cache.containsKey(s)) {
            return cache.get(s);
        } else {
            List<SurveyPath> retval = new ArrayList<>();
            Map<Boolean, List<Block>> partitionedBlocks = Interpreter.partitionBlocks(s);
            List<Block> topLevelRandomizableBlocks = partitionedBlocks.get(true);
            List<Block> nonrandomizableBlocks = partitionedBlocks.get(false);
            Collections.sort(nonrandomizableBlocks);
            SurveyDAG dag = getDag(nonrandomizableBlocks);
            SurveyMan.LOGGER.info("Computing paths for survey having DAG with " + dag.size() + " paths through fixed blocks.");
            if (dag.size() == 1 && dag.get(0).isEmpty()) {
                retval.add(new SurveyPath(topLevelRandomizableBlocks));
                return retval;
            }
            for (SurveyPath blist : dag) {
                if (blist.isEmpty())
                    continue;
                blist.addAll(topLevelRandomizableBlocks);
                retval.add(new SurveyPath(blist));
            }
            assert retval.size() > 0 : String.format("No paths found through Survey %s", s.toString());
            if (retval.size() > 1)
                SurveyMan.LOGGER.info(String.format("Computed %d paths through the survey.", retval.size()));
            cache.put(s, retval);
            return retval;
        }
    }

    public int maximumPathLength() {
        int max = Integer.MIN_VALUE;
        for (SurveyPath path: this) {
            int pathLength = path.getQuestionsFromPath().size();
            if (pathLength > max) {
                max = pathLength;
            }
        }
        SurveyMan.LOGGER.info(String.format("Survey %s has maximum path length of %d", survey.sourceName, max));
        return max;

    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SurveyDAG) {
            SurveyDAG that = (SurveyDAG) o;
            if (this.size() != that.size()) return false;
            for (SurveyPath thisPath: this) {
                boolean found = false;
                for (SurveyPath thatPath: that) {
                    if (thisPath.equals(thatPath)) {
                        found = true; break;
                    }
                }
                if (!found) return false;
            }
            return true;
        } else return false;
    }

}
