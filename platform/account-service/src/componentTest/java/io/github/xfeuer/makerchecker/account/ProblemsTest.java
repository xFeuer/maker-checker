package io.github.xfeuer.makerchecker.account;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.assertj.RestTestClientResponse;

/**
 * The service answers an error with a problem document, also where no code of its own runs. See ADR 0010.
 *
 * <p>The chassis does the work and has the tests for every kind of error. These show that the service has it, with
 * one error for each way out of an application: Spring MVC, the error page of the server, and Tomcat.
 */
@ComponentTest
class ProblemsTest {

    @Test
    void answersAPathThatNoOperationHas(@Autowired RestTestClient client) {
        RestTestClientResponse response =
                RestTestClientResponse.from(client.get().uri("/v1/nowhere").exchange());

        assertProblem(response, HttpStatus.NOT_FOUND, """
            {"type": "about:blank", "title": "Not Found", "status": 404}
            """);
    }

    @Test
    void answersAnOperationOfTheContractWithAProblemThatTheContractAllows(@Autowired RestTestClient client) {
        // The status exists, but not as text. The contract has no 406 for it, so its default response holds this
        // one: the client checks the problem against the schema of the contract.
        RestTestClientResponse response = RestTestClientResponse.from(
                client.get().uri("/v1/status").accept(MediaType.TEXT_PLAIN).exchange());

        assertProblem(response, HttpStatus.NOT_ACCEPTABLE, """
            {"type": "about:blank", "title": "Not Acceptable", "status": 406}
            """);
    }

    @Test
    void answersAMethodThatTheServletRefuses(@Autowired RestTestClient client) {
        // The servlet sends a TRACE request to the error page of the server. Spring MVC never sees it.
        RestTestClientResponse response = RestTestClientResponse.from(
                client.method(HttpMethod.TRACE).uri("/v1/status").exchange());

        assertProblem(response, HttpStatus.METHOD_NOT_ALLOWED, """
            {"type": "about:blank", "title": "Method Not Allowed", "status": 405}
            """);
    }

    @Test
    void answersARequestThatTomcatRefuses(@Autowired RestTestClient client, @Value("${local.server.port}") int port) {
        // Tomcat takes no encoded slash in a path. The address is given whole, so that the client sends it as it is.
        URI encodedSlash = URI.create("http://localhost:%d/v1/accounts%%2Fstatus".formatted(port));

        RestTestClientResponse response =
                RestTestClientResponse.from(client.get().uri(encodedSlash).exchange());

        assertProblem(response, HttpStatus.BAD_REQUEST, """
            {
              "type": "https://xfeuer.github.io/maker-checker/problems/invalid-request",
              "title": "The request breaks the contract.",
              "status": 400,
              "errors": [{"detail": "The server cannot read the request as HTTP that it accepts."}]
            }
            """);
    }

    private static void assertProblem(RestTestClientResponse response, HttpStatus status, String json) {
        assertThat(response)
                .hasStatus(status)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .isStrictlyEqualTo(json);
    }
}
