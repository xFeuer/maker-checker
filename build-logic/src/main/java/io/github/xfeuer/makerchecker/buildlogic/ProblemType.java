package io.github.xfeuer.makerchecker.buildlogic;

import java.util.List;

/**
 * One problem type, as the specifications name it. See ADR 0010.
 *
 * @param name the last part of the type, in kebab-case
 * @param type the address that identifies the problem
 * @param status the status code of every response with this problem
 * @param title the few words that name the problem
 * @param examples what the specifications say about their examples of the problem
 * @param occurrences the operations that answer with the problem
 */
record ProblemType(
        String name, String type, int status, String title, List<Example> examples, List<Occurrence> occurrences) {

    /** An example of the problem in a specification. The description is empty when the example has none. */
    record Example(String summary, String description) {}

    /** An operation that answers with the problem. */
    record Occurrence(String specification, String method, String path, String summary) {}
}
