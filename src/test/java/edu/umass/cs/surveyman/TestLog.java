package edu.umass.cs.surveyman;

import edu.umass.cs.surveyman.input.exceptions.SyntaxException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import edu.umass.cs.surveyman.utils.Slurpie;
import java.io.IOException;

public class TestLog {

    protected Logger LOGGER = LogManager.getLogger(TestLog.class.getName());
    private final String TEST_FOLDER = "./src/test/resources/";

    public String[] testsFiles;
    public char[] separators;
    public boolean[] outcome;

    public TestLog()
            throws IOException,
            SyntaxException
    {
        String[] testData = Slurpie.slurp("test_data").split(System.getProperty("line.separator"));
        this.testsFiles = new String[testData.length];
        this.separators = new char[testData.length];
        this.outcome = new boolean[testData.length];
        for (int i = 0 ; i < testData.length ; i++) {
            String[] stuff = testData[i].split("\\s+");
            this.testsFiles[i] = TEST_FOLDER + stuff[0];
            this.outcome[i] = Boolean.parseBoolean(stuff[2]);
            if (stuff[1].equals(","))
                this.separators[i] = '\u002c';
            else if (stuff[1].equals("\t") || stuff[1].equals("\\t"))
                this.separators[i] = '\u0009';
            else throw new SyntaxException("Unknown delimiter: " + stuff[1]);
        }
    }

    public void init(Class cls)
    {
    }
}
