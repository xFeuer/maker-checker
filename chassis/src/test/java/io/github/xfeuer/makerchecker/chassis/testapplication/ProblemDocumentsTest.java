package io.github.xfeuer.makerchecker.chassis.testapplication;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.assertj.RestTestClientResponse;

/**
 * The errors that reach Spring, on a real port: those that the code of an application raises, those that Spring MVC
 * finds in a request, and those that arrive at the error page of the server.
 *
 * <p>Every comparison is strict: a problem has the members of the contract and no other.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class ProblemDocumentsTest {

    @Autowired
    RestTestClient client;

    @Test
    void leavesAResponseThatIsNoErrorAsItIs() {
        assertThat(get("/works"))
                .hasStatusOk()
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .bodyJson()
                .isStrictlyEqualTo("""
                    {"answer": "yes"}
                    """);
    }

    @Test
    void answersARefusalWithItsType() {
        assertProblem(get("/refuses"), 422, """
            {
              "type": "https://xfeuer.github.io/maker-checker/problems/teapot-empty",
              "title": "The teapot is empty.",
              "status": 422
            }
            """);
    }

    @Test
    void addsTheDetailOfARefusal() {
        assertProblem(get("/refuses-with-a-detail"), 422, """
            {
              "type": "https://xfeuer.github.io/maker-checker/problems/teapot-empty",
              "title": "The teapot is empty.",
              "status": 422,
              "detail": "The last cup went at noon."
            }
            """);
    }

    @Test
    void listsEveryViolationThatTheCodeFound() {
        assertProblem(get("/finds-violations"), 400, """
            {
              "type": "https://xfeuer.github.io/maker-checker/problems/invalid-request",
              "title": "The request breaks the contract.",
              "status": 400,
              "errors": [
                {"detail": "The number of cups is 1 to 12.", "pointer": "#/cups"},
                {"detail": "The header is required.", "parameter": "Idempotency-Key"}
              ]
            }
            """);
    }

    @Test
    void saysNothingAboutAFailureThatNoCodeExpected() {
        assertProblem(get("/fails"), 500, """
            {"type": "about:blank", "title": "Internal Server Error", "status": 500}
            """);
    }

    @Test
    void keepsAStatusThatTheCodeNamesTheWayOfSpringAndDropsItsReason() {
        assertProblem(get("/conflicts"), 409, """
            {"type": "about:blank", "title": "Conflict", "status": 409}
            """);
    }

    @Test
    void answersAPathThatNoOperationHas() {
        assertProblem(get("/nowhere"), 404, """
            {"type": "about:blank", "title": "Not Found", "status": 404}
            """);
    }

    @Test
    void answersAMethodThatThePathDoesNotHaveAndNamesTheOnesItHas() {
        RestTestClientResponse response =
                RestTestClientResponse.from(client.post().uri("/works").exchange());

        assertProblem(response, 405, """
            {"type": "about:blank", "title": "Method Not Allowed", "status": 405}
            """);
        assertThat(response).headers().hasValue(HttpHeaders.ALLOW, "GET");
    }

    @Test
    void answersARequestForAMediaTypeThatItDoesNotWrite() {
        RestTestClientResponse response = RestTestClientResponse.from(
                client.get().uri("/works").accept(MediaType.TEXT_PLAIN).exchange());

        assertProblem(response, 406, """
            {"type": "about:blank", "title": "Not Acceptable", "status": 406}
            """);
    }

    @Test
    void answersABodyOfAMediaTypeThatItDoesNotRead() {
        RestTestClientResponse response = RestTestClientResponse.from(client.post()
                .uri("/orders")
                .contentType(MediaType.TEXT_PLAIN)
                .body("two cups of green tea")
                .exchange());

        assertProblem(response, 415, """
            {"type": "about:blank", "title": "Unsupported Media Type", "status": 415}
            """);
    }

    @Test
    void namesAHeaderThatIsMissing() {
        assertProblem(get("/needs-a-header"), 400, invalidRequest("""
            {"detail": "The header is required.", "parameter": "X-Table"}
            """));
    }

    @Test
    void namesAQueryParameterThatIsMissing() {
        assertProblem(get("/needs-a-parameter"), 400, invalidRequest("""
            {"detail": "The parameter is required.", "parameter": "limit"}
            """));
    }

    @Test
    void namesAQueryParameterOfTheWrongFormat() {
        assertProblem(get("/needs-a-parameter?limit=ten"), 400, invalidRequest("""
            {"detail": "The value does not have the format of the parameter.", "parameter": "limit"}
            """));
    }

    @Test
    void namesAPathParameterOfTheWrongFormat() {
        assertProblem(get("/cups/many"), 400, invalidRequest("""
            {"detail": "The value does not have the format of the parameter.", "parameter": "number"}
            """));
    }

    @Test
    void answersABodyThatIsNoJson() {
        RestTestClientResponse response = RestTestClientResponse.from(client.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"tea\": ")
                .exchange());

        assertProblem(response, 400, invalidRequest("""
            {"detail": "The body is not JSON of the form that the contract asks for."}
            """));
    }

    @Test
    void answersAFailureInAFilter() {
        assertProblem(get("/filter-fails"), 500, """
            {"type": "about:blank", "title": "Internal Server Error", "status": 500}
            """);
    }

    @Test
    void answersAnErrorThatAFilterSends() {
        assertProblem(get("/filter-forbids"), 403, """
            {"type": "about:blank", "title": "Forbidden", "status": 403}
            """);
    }

    @Test
    void answersARequestForTheErrorPageItselfAsAnyUnknownPath() {
        assertProblem(get("/error"), 404, """
            {"type": "about:blank", "title": "Not Found", "status": 404}
            """);
    }

    private RestTestClientResponse get(String path) {
        return RestTestClientResponse.from(client.get().uri(path).exchange());
    }

    private static String invalidRequest(String violation) {
        return """
            {
              "type": "https://xfeuer.github.io/maker-checker/problems/invalid-request",
              "title": "The request breaks the contract.",
              "status": 400,
              "errors": [%s]
            }
            """.formatted(violation);
    }

    private static void assertProblem(RestTestClientResponse response, int status, String json) {
        assertThat(response)
                .hasStatus(HttpStatus.valueOf(status))
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .isStrictlyEqualTo(json);
    }
}
