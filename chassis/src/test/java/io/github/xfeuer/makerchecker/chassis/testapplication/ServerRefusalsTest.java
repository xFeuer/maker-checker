package io.github.xfeuer.makerchecker.chassis.testapplication;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonContent;

/**
 * The errors that never reach Spring MVC: Tomcat refuses these requests itself, or the servlet does. Without the
 * chassis the answer is a page of HTML, or the error format of Spring Boot.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ServerRefusalsTest {

    private static final String NOT_HTTP = """
        {
          "type": "https://xfeuer.github.io/maker-checker/problems/invalid-request",
          "title": "The request breaks the contract.",
          "status": 400,
          "errors": [{"detail": "The server cannot read the request as HTTP that it accepts."}]
        }
        """;

    @Value("${local.server.port}")
    int port;

    @Test
    void answersATargetWithACharacterThatHttpDoesNotAllow() throws IOException {
        RawHttp.Reply reply = send("GET /cups/{number} HTTP/1.1", "Host: localhost");

        assertProblem(reply, 400, NOT_HTTP);
    }

    @Test
    void answersATargetWithAnEncodedSlash() throws IOException {
        RawHttp.Reply reply = send("GET /cups/1%2F2 HTTP/1.1", "Host: localhost");

        assertProblem(reply, 400, NOT_HTTP);
    }

    @Test
    void answersARequestWithoutAHost() throws IOException {
        RawHttp.Reply reply = send("GET /works HTTP/1.1");

        assertProblem(reply, 400, NOT_HTTP);
    }

    @Test
    void answersAHeaderThatIsLargerThanTheServerTakes() throws IOException {
        RawHttp.Reply reply = send("GET /works HTTP/1.1", "Host: localhost", "X-Padding: " + "a".repeat(20_000));

        assertProblem(reply, 400, NOT_HTTP);
    }

    @Test
    void answersAVersionOfHttpThatTheServerDoesNotSpeak() throws IOException {
        RawHttp.Reply reply = send("GET /works HTTP/3.0", "Host: localhost");

        assertProblem(reply, 505, """
            {"type": "about:blank", "title": "HTTP Version not supported", "status": 505}
            """);
    }

    @Test
    void answersAMethodThatTheServletRefuses() throws IOException {
        RawHttp.Reply reply = send("TRACE /works HTTP/1.1", "Host: localhost");

        assertProblem(reply, 405, """
            {"type": "about:blank", "title": "Method Not Allowed", "status": 405}
            """);
        assertThat(reply.headers()).containsKey("Allow");
    }

    private RawHttp.Reply send(String requestLine, String... headers) throws IOException {
        StringBuilder request = new StringBuilder(requestLine).append("\r\n");
        for (String header : headers) {
            request.append(header).append("\r\n");
        }
        return RawHttp.send(port, request.append("Connection: close\r\n\r\n").toString());
    }

    private static void assertProblem(RawHttp.Reply reply, int status, String json) {
        assertThat(reply.status()).isEqualTo(status);
        assertThat(MediaType.parseMediaType(reply.headers().get("Content-Type")))
                .matches(MediaType.APPLICATION_PROBLEM_JSON::isCompatibleWith, "is a problem document");
        assertThat(new JsonContent(reply.body())).isStrictlyEqualTo(json);
    }
}
