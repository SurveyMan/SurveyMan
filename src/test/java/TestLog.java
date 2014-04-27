import org.apache.log4j.*;
import input.Slurpie;

import java.io.IOException;

public class TestLog {

    protected final Logger LOGGER = Logger.getRootLogger();

    public String[] testsFiles;
    public char[] separators;

    public TestLog() {
        try {
            String[] testData = Slurpie.slurp("test_data").split(System.getProperty("line.separator"));
            this.testsFiles = new String[testData.length];
            this.separators = new char[testData.length];
            for (int i = 0 ; i < testData.length ; i++) {
                String[] stuff = testData[i].split("\\s+");
                this.testsFiles[i] = stuff[0];
                this.separators[i] = stuff[1].charAt(0);
            }
        } catch (IOException io) {
            io.printStackTrace();
            System.exit(0);
        }
    }

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
            throw new RuntimeException(io);
        }
    }
}
