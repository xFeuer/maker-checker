package io.github.xfeuer.makerchecker.chassis.problem;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.json.JsonMapper;

/**
 * A problem document of RFC 9457, with exactly the members that the contracts give it. See ADR 0010 and ADR 0018.
 *
 * <p>Every error response of an application is one of these, whoever found the error: the code of the application,
 * Spring MVC, the error page of the server, or Tomcat.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"type", "title", "status", "detail", "errors"})
record Problem(URI type, String title, int status, String detail, List<Violation> errors) {

    static final MediaType MEDIA_TYPE = MediaType.APPLICATION_PROBLEM_JSON;

    /** For the one place that writes a problem without Spring: the valve of Tomcat. */
    private static final JsonMapper JSON = JsonMapper.builder().build();

    static Problem of(ProblemType type) {
        return of(type, null);
    }

    static Problem of(ProblemType type, String detail) {
        return new Problem(type.type(), type.title(), type.status(), detail, null);
    }

    /** A request that breaks the contract. Only such a problem has the member {@code errors}. */
    static Problem ofViolations(List<Violation> violations) {
        ProblemType type = CommonProblem.INVALID_REQUEST;
        return new Problem(type.type(), type.title(), type.status(), null, List.copyOf(violations));
    }

    /**
     * The problem for a response that has a status and nothing else to go by. A 400 always lists a violation, so
     * this one names the only thing that is known.
     */
    static Problem ofStatus(int status) {
        if (status == HttpStatus.BAD_REQUEST.value()) {
            return ofViolations(List.of(Violation.of("The server cannot read the request as HTTP that it accepts.")));
        }
        return of(ProblemType.blank(HttpStatusCode.valueOf(status)));
    }

    ResponseEntity<Object> toResponse(HttpHeaders headers) {
        return ResponseEntity.status(status)
                .headers(headers)
                .contentType(MEDIA_TYPE)
                .body(this);
    }

    String toJson() {
        return JSON.writeValueAsString(this);
    }
}
