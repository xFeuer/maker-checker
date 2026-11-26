package io.github.xfeuer.makerchecker.chassis.problem;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Objects;

/**
 * One violation of the contract, in a problem with the status 400. It says what is wrong, and it names its place
 * when it has one: a pointer into the body, or the name of a parameter. See ADR 0010.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"detail", "pointer", "parameter"})
public record Violation(String detail, String pointer, String parameter) {

    public Violation {
        Objects.requireNonNull(detail, "detail");
    }

    /**
     * A violation in the body. The pointer is a JSON Pointer, written as a URI fragment, such as
     * {@code #/amount/amount}.
     */
    public static Violation inBody(String pointer, String detail) {
        return new Violation(detail, Objects.requireNonNull(pointer, "pointer"), null);
    }

    /** A violation in a header, a query parameter or a path parameter, by the name of that parameter. */
    public static Violation inParameter(String name, String detail) {
        return new Violation(detail, null, Objects.requireNonNull(name, "name"));
    }

    /** A violation without a single place, such as a body that is no JSON at all. */
    public static Violation of(String detail) {
        return new Violation(detail, null, null);
    }
}
