package io.github.xfeuer.makerchecker.chassis.testapplication;

import io.github.xfeuer.makerchecker.chassis.problem.InvalidRequestException;
import io.github.xfeuer.makerchecker.chassis.problem.ProblemException;
import io.github.xfeuer.makerchecker.chassis.problem.ProblemType;
import io.github.xfeuer.makerchecker.chassis.problem.Violation;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * A small application on top of the chassis, as a service is one. Each of its operations ends in another kind of
 * error.
 *
 * <p>It lives in a package of its own, as a service does. So Spring does not find the classes of the chassis by
 * scanning, and the tests prove that the chassis configures itself.
 */
@SpringBootApplication
class TestApplication {

    /** A type of problem, declared as an application declares its own. */
    enum TeaProblem implements ProblemType {
        TEAPOT_EMPTY("teapot-empty", 422, "The teapot is empty.");

        private final URI type;
        private final int status;
        private final String title;

        TeaProblem(String name, int status, String title) {
            this.type = ProblemType.named(name);
            this.status = status;
            this.title = title;
        }

        @Override
        public URI type() {
            return type;
        }

        @Override
        public int status() {
            return status;
        }

        @Override
        public String title() {
            return title;
        }
    }

    record Order(String tea, int cups) {}

    @RestController
    static class Operations {

        @GetMapping("/works")
        Map<String, String> works() {
            return Map.of("answer", "yes");
        }

        @GetMapping("/breaks-the-contract")
        Map<String, String> breaksTheContract() {
            // The contract of the test application asks for a header here, and it has no kettle in the body.
            return Map.of("answer", "yes", "kettle", "hot");
        }

        @GetMapping("/refuses")
        void refuses() {
            throw new ProblemException(TeaProblem.TEAPOT_EMPTY);
        }

        @GetMapping("/refuses-with-a-detail")
        void refusesWithADetail() {
            throw new ProblemException(TeaProblem.TEAPOT_EMPTY, "The last cup went at noon.");
        }

        @GetMapping("/finds-violations")
        void findsViolations() {
            throw new InvalidRequestException(List.of(
                    Violation.inBody("#/cups", "The number of cups is 1 to 12."),
                    Violation.inParameter("Idempotency-Key", "The header is required.")));
        }

        @GetMapping("/fails")
        void fails() {
            throw new IllegalStateException("The password of the database is hunter2.");
        }

        @GetMapping("/conflicts")
        void conflicts() {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A reason in the words of the code.");
        }

        @GetMapping("/cups/{number}")
        Map<String, Integer> cups(@PathVariable("number") int number) {
            return Map.of("cups", number);
        }

        @GetMapping("/needs-a-header")
        Map<String, String> needsAHeader(@RequestHeader("X-Table") String table) {
            return Map.of("table", table);
        }

        @GetMapping("/needs-a-parameter")
        Map<String, Integer> needsAParameter(@RequestParam("limit") int limit) {
            return Map.of("limit", limit);
        }

        @PostMapping("/orders")
        Order order(@RequestBody Order order) {
            return order;
        }
    }

    /** Ends two requests before Spring MVC sees them, as a filter of an application can. */
    @Bean
    Filter trouble() {
        return (request, response, chain) -> {
            String path = ((HttpServletRequest) request).getRequestURI();
            if (path.equals("/filter-fails")) {
                throw new IllegalStateException("The filter knows the password too.");
            }
            if (path.equals("/filter-forbids")) {
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            chain.doFilter(request, response);
        };
    }
}
