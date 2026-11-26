package io.github.xfeuer.makerchecker.chassis.problem;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every exception that reaches Spring MVC into a problem document.
 *
 * <p>Spring finds the errors of a request and names their status. The document is ours: the one of Spring leaves
 * out the type, and its details say how the application is built. See ADR 0018.
 *
 * <p>It comes last among the advice of an application, because it takes every exception.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
class ProblemExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ProblemExceptionHandler.class);

    /** A refusal that the code of the application raised. */
    @ExceptionHandler
    ResponseEntity<Object> refuse(ProblemException refusal) {
        Problem problem = refusal instanceof InvalidRequestException invalid
                ? Problem.ofViolations(invalid.violations())
                : Problem.of(refusal.type(), refusal.detail());
        return problem.toResponse(new HttpHeaders());
    }

    /** A failure that no code expected. The response says nothing about it, and the log says everything. */
    @ExceptionHandler
    ResponseEntity<Object> fail(Exception failure) {
        LOG.error("A request failed with an exception that no code expected.", failure);
        return Problem.of(ProblemType.blank(HttpStatus.INTERNAL_SERVER_ERROR)).toResponse(new HttpHeaders());
    }

    /**
     * What Spring MVC itself reports about a request, such as a method that the path does not have. Spring names
     * the status and the headers, such as {@code Allow}.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (request instanceof ServletWebRequest servlet
                && servlet.getResponse() != null
                && servlet.getResponse().isCommitted()) {
            // A part of another response is on its way to the client. Nothing can be said anymore.
            LOG.warn("A request failed after its response was committed.", exception);
            return null;
        }
        if (status.is5xxServerError()) {
            LOG.error("A request failed inside Spring MVC.", exception);
        }
        Problem problem = status.value() == HttpStatus.BAD_REQUEST.value()
                ? Problem.ofViolations(List.of(violation(exception)))
                : Problem.of(ProblemType.blank(status));
        return problem.toResponse(headers);
    }

    /** Spring stops at the first thing that is wrong with a request, so it knows one violation. */
    private static Violation violation(Exception exception) {
        return switch (exception) {
            case MissingRequestHeaderException missing ->
                Violation.inParameter(missing.getHeaderName(), "The header is required.");
            case MissingServletRequestParameterException missing ->
                Violation.inParameter(missing.getParameterName(), "The parameter is required.");
            case MethodArgumentTypeMismatchException mismatch ->
                Violation.inParameter(mismatch.getName(), "The value does not have the format of the parameter.");
            case HttpMessageNotReadableException _ ->
                Violation.of("The body is not JSON of the form that the contract asks for.");
            default -> Violation.of("The request does not keep the contract.");
        };
    }
}
