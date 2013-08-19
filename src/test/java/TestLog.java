import csv.CSVLexer;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import scala.Tuple2;

import java.io.IOException;

public class TestLog {

    protected final Logger LOGGER = Logger.getRootLogger();
    private FileAppender txtHandler;

    public Tuple2[] tests = { new Tuple2("data/tests/test1_toobig.csv", ",")
            , new Tuple2("data/tests/test2.csv", "\t")
            , new Tuple2("data/tests/test3.csv", ":")
    };

    public void init(Class cls){
        LOGGER.setLevel(Level.ALL);
        try {
            txtHandler = new FileAppender(new SimpleLayout(), String.format("logs/%s.log", cls.getName()));
            txtHandler.setEncoding(CSVLexer.encoding);
            txtHandler.setAppend(false);
            LOGGER.addAppender(txtHandler);
        }
        catch (IOException io) {
            System.err.println(io.getMessage());
            System.exit(-1);
        }
    }
}
