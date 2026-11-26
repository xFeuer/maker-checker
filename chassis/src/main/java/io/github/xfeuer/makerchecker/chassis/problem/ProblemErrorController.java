package io.github.xfeuer.makerchecker.chassis.problem;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Answers what arrives at the error page of the server: an error that no handler of Spring MVC saw, such as an
 * exception in a filter. It takes the place of the error controller of Spring Boot, which answers in a format of
 * its own. See ADR 0018.
 */
@RestController
class ProblemErrorController implements ErrorController {

    @RequestMapping("${server.error.path:${error.path:/error}}")
    ResponseEntity<Object> error(HttpServletRequest request) {
        // The server names the status when it sends a request here. Without one, a client asked for this path
        // itself, and no contract has it.
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int code = status instanceof Integer sent ? sent : HttpStatus.NOT_FOUND.value();
        return Problem.ofStatus(code).toResponse(new HttpHeaders());
    }
}
