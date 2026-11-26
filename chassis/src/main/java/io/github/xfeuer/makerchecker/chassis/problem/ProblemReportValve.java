package io.github.xfeuer.makerchecker.chassis.problem;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ErrorReportValve;
import org.apache.coyote.ActionCode;

/**
 * Answers a request that Tomcat refuses before any code of Spring runs, such as one with a character in its target
 * that HTTP does not allow. Without this valve Tomcat answers with a page of HTML. See ADR 0018.
 *
 * <p>Tomcat creates the valve by the name of its class, so the class is public and knows nothing of the
 * application.
 */
public class ProblemReportValve extends ErrorReportValve {

    @Override
    protected void report(Request request, Response response, Throwable throwable) {
        int status = response.getStatus();
        // As the valves of Tomcat do: nothing for a response that is no error, that has a body, or that another
        // valve has reported.
        if (status < 400 || response.getContentWritten() > 0 || !response.setErrorReported()) {
            return;
        }
        AtomicBoolean writable = new AtomicBoolean(false);
        response.getCoyoteResponse().action(ActionCode.IS_IO_ALLOWED, writable);
        if (!writable.get()) {
            return;
        }
        try {
            response.setContentType(Problem.MEDIA_TYPE.toString());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            Writer writer = response.getReporter();
            if (writer != null) {
                writer.write(Problem.ofStatus(status).toJson());
                response.finishResponse();
            }
        } catch (IOException | IllegalStateException gone) {
            // The client is gone, or the response cannot be written anymore. There is nobody left to tell.
        }
    }
}
