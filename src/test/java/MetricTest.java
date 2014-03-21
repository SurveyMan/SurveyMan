import csv.CSVLexer;
import csv.CSVParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import qc.QCMetrics;
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
    public void testAveragePath() throws Exception{
        Survey s = new CSVParser(new CSVLexer(pathTest, sep)).parse();
        double avg = QCMetrics.averagePathLength(s);
        LOGGER.info(String.format("Average path length for survey %s : %f", s.sourceName, avg));
    }
}
