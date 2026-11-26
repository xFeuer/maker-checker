package io.github.xfeuer.makerchecker.chassis.problem;

import java.util.List;

/**
 * Refuses a request that breaks the contract. The problem has the status 400 and the type {@code invalid-request},
 * and it lists every violation.
 */
public class InvalidRequestException extends ProblemException {

    private static final long serialVersionUID = 1L;

    private final transient List<Violation> violations;

    public InvalidRequestException(Violation violation) {
        this(List.of(violation));
    }

    public InvalidRequestException(List<Violation> violations) {
        super(CommonProblem.INVALID_REQUEST);
        if (violations.isEmpty()) {
            throw new IllegalArgumentException("A request that breaks the contract has at least one violation.");
        }
        this.violations = List.copyOf(violations);
    }

    public List<Violation> violations() {
        return violations;
    }
}
