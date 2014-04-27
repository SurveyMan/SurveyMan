import input.csv.CSVLexer;
import input.csv.CSVParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import qc.QCMetrics;
import qc.Metrics;
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
    QCMetrics metrics = new Metrics();

    public MetricTest(){
        super.init(this.getClass());
    }

    @Test
    public void testMinPath() throws Exception{
        Survey s = new CSVParser(new CSVLexer(pathTest, sep)).parse();
        int calculatedMinPath = metrics.minimumPathLength(s);
        assert calculatedMinPath==minPath : String.format("Expected path length %d; got path length %d in survey %s"
                , minPath, calculatedMinPath, pathTest);
    }

    @Test
    public void testMaxPath() throws Exception{
        Survey s = new CSVParser(new CSVLexer(pathTest, sep)).parse();
        int calculatedMaxPath = metrics.maximumPathLength(s);
        assert calculatedMaxPath==maxPath : String.format("Expected path length %d; got path length %d in survey %s"
                , maxPath, calculatedMaxPath, pathTest);
    }

    @Test
    public void empiricalMinMaxPath() throws Exception{
        for ( int i = 0 ; i < testsFiles.length ; i++ ) {
            CSVLexer lexer = new CSVLexer(testsFiles[i], String.valueOf(separators[i]));
            CSVParser parser = new CSVParser(lexer);
            Survey survey = parser.parse();
            int min = metrics.minimumPathLength(survey);
            int max = metrics.maximumPathLength(survey);
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
        double avg = metrics.averagePathLength(s);
        LOGGER.info(String.format("Average path length for survey %s : %f", s.sourceName, avg));
    }
}
