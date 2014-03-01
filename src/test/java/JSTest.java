import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class JSTest extends TestLog {

    public JSTest(){
        super.init(this.getClass());
    }

    @Test
    public void testPaths() {
        // generate test paths through the survey that pick the lexicographically first option for
        // each test survey. Take the test. Assert that the results are equivalent
        for (int i = 0 ; i < super.testsFiles.length ; i++) {
            
        }
    }

}
