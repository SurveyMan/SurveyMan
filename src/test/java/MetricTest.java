import input.csv.CSVLexer;
import input.csv.CSVParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import qc.QCMetrics;
import qc.RandomRespondent;
import survey.Survey;

@RunWith(JUnit4.class)
public class MetricTest extends TestLog {
    // test path lengths

    String pathTest = "data/tests/pathTest.csv";
    String sep = ",";
    int totalQuestions = 21;
    int minPath = 10;
    int maxPath = 19;
    double avg = (19 + 18 + 15 + 14 + 15 + 13 + 10) / 7.0;

    public MetricTest(){
        super.init(this.getClass());
    }

    @Test
    public void testMinPath() throws Exception{
        Survey s = new CSVParser(new CSVLexer(pathTest, sep)).parse();
        int calculatedMinPath = QCMetrics.minimumPathLength(s);
        assert(calculatedMinPath==minPath);
    }

    @Test
    public void testMaxPath() throws Exception{
        Survey s = new CSVParser(new CSVLexer(pathTest, sep)).parse();
        int calculatedMaxPath = QCMetrics.maximumPathLength(s);
        assert(calculatedMaxPath==maxPath);
    }

    @Test
    public void empiricalMinMaxPath() throws Exception{
        for ( int i = 0 ; i < testsFiles.length ; i++ ) {
            CSVLexer lexer = new CSVLexer(testsFiles[i], String.valueOf(separators[i]));
            CSVParser parser = new CSVParser(lexer);
            Survey survey = parser.parse();
            int min = QCMetrics.minimumPathLength(survey);
            int max = QCMetrics.maximumPathLength(survey);
            int empMin = Integer.MAX_VALUE;
            int empMax = Integer.MIN_VALUE;
            int iterations = 1000;
            for (int j = 0 ; j < iterations; j++){
                int k = new RandomRespondent(survey, RandomRespondent.AdversaryType.UNIFORM).response.responses.size();
                if (k < empMin)
                    empMin = k;
                if (k > empMax)
                    empMax = k;
            }
            assert min==empMin : String.format("Computed min path of %d; observed min path of %d over %d iterations in survey %s", min, empMin, iterations, survey.sourceName);
            assert max==empMax : String.format("Computed max path of %d; observed max path of %d over %d iterations in survey %s", max, empMax, iterations, survey.sourceName);
        }
    }

    @Test
    public void testAveragePath() throws Exception{
        Survey s = new CSVParser(new CSVLexer(pathTest, sep)).parse();
        double avg = QCMetrics.averagePathLength(s);
        LOGGER.info(String.format("Average path length for survey %s : %f", s.sourceName, avg));
    }
}
