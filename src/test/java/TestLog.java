import org.apache.log4j.*;
import java.io.IOException;

public class TestLog {

    protected final Logger LOGGER = Logger.getRootLogger();

    public String[] testsFiles = { //"data/tests/test1_toobig.csv",
 //            "data/tests/test3.csv"
 //           , "data/tests/test4.csv"
 //           , "data/tests/test5.csv",
             "data/tests/sample_survey_wording.csv"
    };
    public char[] separators = {
        //',', ',', ',',
            ';'
    };

    public void init(Class cls){
        LOGGER.setLevel(Level.ALL);
        try {
            FileAppender txtHandler = new FileAppender(new PatternLayout("%d{dd MMM yyyy HH:mm:ss,SSS}\t%-5p [%t]: %m%n"), String.format("logs/%s.log", cls.getName()));
            txtHandler.setEncoding("UTF-8");
            txtHandler.setAppend(false);
            LOGGER.addAppender(txtHandler);
        }
        catch (IOException io) {
            System.err.println(io.getMessage());
            System.exit(-1);
        }
    }
}
