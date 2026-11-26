package io.github.xfeuer.makerchecker.chassis.problem;

import java.net.URI;
import org.springframework.http.HttpStatusCode;

/**
 * A type of problem, as the contracts name it: an identifier, a status code and a title. See ADR 0010.
 *
 * <p>An application declares the types that it answers with in an enum of its own. The types that every
 * application answers with are in {@link CommonProblem}.
 */
public interface ProblemType {

    /** The identifier of the type: the address of its page, or {@code about:blank}. */
    URI type();

    /** The status code of every response with this type. */
    int status();

    /** A few words that name the type. They are the same for every problem of the type. */
    String title();

    /**
     * The identifier of a type with a name, such as {@code insufficient-funds}: the address of its page on the site
     * of the repository. This is the one place that knows where the site is.
     */
    static URI named(String name) {
        return URI.create("https://xfeuer.github.io/maker-checker/problems/" + name);
    }

    /**
     * The type of a problem that its status code explains fully. Its identifier is {@code about:blank} and its
     * title is the phrase of the status, as RFC 9457 has it.
     */
    static ProblemType blank(HttpStatusCode status) {
        return new BlankProblem(status.value());
    }
}
