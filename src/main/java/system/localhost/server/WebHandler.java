package system.localhost.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface WebHandler {
    void handle(HttpServletRequest request, HttpServletResponse response) throws IOException;
}
