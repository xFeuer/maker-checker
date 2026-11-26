package io.github.xfeuer.makerchecker.chassis.problem;

import java.net.URI;
import org.springframework.http.HttpStatus;

/** The type {@code about:blank}: the status code says everything. */
record BlankProblem(int status) implements ProblemType {

    private static final URI BLANK = URI.create("about:blank");

    @Override
    public URI type() {
        return BLANK;
    }

    @Override
    public String title() {
        HttpStatus known = HttpStatus.resolve(status);
        return known != null ? known.getReasonPhrase() : "Status " + status;
    }
}
