package io.github.xfeuer.makerchecker.chassis.problem;

import java.util.Objects;

/**
 * Refuses a request. The response is a problem document of the given type. See ADR 0010 and ADR 0018.
 *
 * <p>The code that finds a broken rule throws this, wherever it is. The exception is unchecked, so a transaction
 * that it leaves rolls back, and a refused request changes nothing.
 */
public class ProblemException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient ProblemType type;
    private final String detail;

    public ProblemException(ProblemType type) {
        this(type, null);
    }

    /**
     * @param detail a sentence for a person about this one occurrence. It goes into the response, so it never shows
     *     the inside of the application.
     */
    public ProblemException(ProblemType type, String detail) {
        super(detail == null ? type.type().toString() : type.type() + ": " + detail);
        this.type = Objects.requireNonNull(type, "type");
        this.detail = detail;
    }

    public ProblemType type() {
        return type;
    }

    /** The sentence about this occurrence, or {@code null} when the type says everything. */
    public String detail() {
        return detail;
    }
}
