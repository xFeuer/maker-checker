package io.github.xfeuer.makerchecker.chassis.problem;

import java.net.URI;
import org.springframework.http.HttpStatus;

/**
 * The types of problem that every application answers with. The contracts share them in
 * {@code contracts/common/components.yaml}.
 */
public enum CommonProblem implements ProblemType {

    /** The request breaks the contract. Such a problem lists every violation: see {@link InvalidRequestException}. */
    INVALID_REQUEST("invalid-request", HttpStatus.BAD_REQUEST, "The request breaks the contract.");

    private final URI type;
    private final int status;
    private final String title;

    CommonProblem(String name, HttpStatus status, String title) {
        this.type = ProblemType.named(name);
        this.status = status.value();
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
