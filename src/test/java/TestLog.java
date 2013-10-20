import csv.CSVLexer;
import org.apache.log4j.*;
import scala.Tuple2;

import java.io.IOException;

public class TestLog {

    protected final Logger LOGGER = Logger.getRootLogger();
    private FileAppender txtHandler;

    public Tuple2[] tests = { new Tuple2("data/tests/test1_toobig.csv", ",")
            , new Tuple2("data/tests/test2.csv", "\t")
            , new Tuple2("data/tests/test3.csv", ":")
            , new Tuple2("data/tests/test4.csv", ",")
            , new Tuple2("data/tests/test5.csv", ",")
    };

    public void init(Class cls){
        LOGGER.setLevel(Level.ALL);
        try {
            txtHandler = new FileAppender(new PatternLayout("%d{dd MMM yyyy HH:mm:ss,SSS}\t%-5p [%t]: %m%n"), String.format("logs/%s.log", cls.getName()));
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
