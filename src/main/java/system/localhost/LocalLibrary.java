package system.localhost;

import survey.Survey;
import system.Library;

public class LocalLibrary extends Library {
    public static final int port = 8000;
    public static final String jshome = "src/javascript";

    public LocalLibrary() {
    }

    public LocalLibrary(Survey survey) {
        super(survey);
    }
}
