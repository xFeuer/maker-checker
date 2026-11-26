package io.github.xfeuer.makerchecker.buildlogic;

import io.github.xfeuer.makerchecker.buildlogic.ProblemType.Example;
import io.github.xfeuer.makerchecker.buildlogic.ProblemType.Occurrence;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Collects the problem types that the specifications name. See ADR 0010 and ADR 0015.
 *
 * <p>A specification names a problem in the examples of its error responses. The catalog reads them from the
 * specifications as vacuum bundles them: JSON documents in which every reference to another file is resolved.
 *
 * <p>A problem whose status code says everything has the type {@code about:blank}. It is no type of our own, so
 * the catalog leaves it out.
 */
final class ProblemCatalog {

    private static final List<String> METHODS = List.of("get", "put", "post", "delete");
    private static final String PROBLEM_DOCUMENT = "application/problem+json";
    private static final String NO_TYPE_OF_OUR_OWN = "about:blank";

    /** The name becomes the name of a file, so it is checked here once more. */
    private static final Pattern NAME = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

    private ProblemCatalog() {}

    /**
     * The problem types of the specifications, in the order of their names.
     *
     * @param specifications the bundled specifications, each as a JSON reader gives it: maps, lists and values
     * @throws IllegalArgumentException when the specifications do not agree on a type. The message has one line
     *     for each fault.
     */
    static List<ProblemType> in(List<?> specifications) {
        Map<String, Collected> collected = new TreeMap<>();
        Set<String> faults = new TreeSet<>();
        for (Object specification : specifications) {
            collect(map(specification), collected, faults);
        }

        Set<String> places = new TreeSet<>();
        collected.forEach((type, problem) -> {
            places.add(type.substring(0, type.lastIndexOf('/') + 1));
            if (problem.titles.size() > 1) {
                faults.add("%s has more than one title: %s".formatted(type, String.join(" | ", problem.titles)));
            }
            if (problem.statuses.size() > 1) {
                faults.add("%s has more than one status: %s".formatted(type, problem.statuses));
            }
        });
        if (places.size() > 1) {
            faults.add("The problem types lie at more than one address: " + String.join(" | ", places));
        }
        if (!faults.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", faults));
        }

        return collected.entrySet().stream()
                .map(entry -> entry.getValue().as(entry.getKey()))
                .sorted(Comparator.comparing(ProblemType::name))
                .toList();
    }

    private static void collect(Map<?, ?> specification, Map<String, Collected> collected, Set<String> faults) {
        String title = text(map(specification.get("info")).get("title"));
        map(specification.get("paths")).forEach((path, item) -> {
            Map<?, ?> operations = resolved(specification, item);
            for (String method : METHODS) {
                Map<?, ?> operation = map(operations.get(method));
                Occurrence occurrence = new Occurrence(
                        title, method.toUpperCase(Locale.ROOT), text(path), text(operation.get("summary")));
                for (Object response : map(operation.get("responses")).values()) {
                    Map<?, ?> body = map(map(resolved(specification, response).get("content"))
                            .get(PROBLEM_DOCUMENT));
                    for (Object example : map(body.get("examples")).values()) {
                        collect(resolved(specification, example), occurrence, collected, faults);
                    }
                }
            }
        });
    }

    private static void collect(
            Map<?, ?> example, Occurrence occurrence, Map<String, Collected> collected, Set<String> faults) {
        Map<?, ?> problem = map(example.get("value"));
        String type = text(problem.get("type"));
        if (type.equals(NO_TYPE_OF_OUR_OWN)) {
            return;
        }
        String place = "%s %s of %s".formatted(occurrence.method(), occurrence.path(), occurrence.specification());
        String name = type.substring(type.lastIndexOf('/') + 1);
        if (!type.contains("/") || !NAME.matcher(name).matches()) {
            faults.add("%s has an example of a problem whose type does not end in a name in kebab-case: %s"
                    .formatted(place, type));
            return;
        }
        if (!(problem.get("status") instanceof Integer status)
                || text(problem.get("title")).isEmpty()) {
            faults.add("%s has an example of %s without a title or without a status.".formatted(place, type));
            return;
        }
        Collected known = collected.computeIfAbsent(type, unused -> new Collected());
        known.titles.add(text(problem.get("title")));
        known.statuses.add(status);
        known.examples.add(new Example(text(example.get("summary")), text(example.get("description"))));
        known.occurrences.add(occurrence);
    }

    /** What the specifications say about one type, before it is known that they agree. */
    private static final class Collected {

        private final Set<String> titles = new TreeSet<>();
        private final Set<Integer> statuses = new TreeSet<>();
        // In the order in which the specifications name them, each one once.
        private final Set<Example> examples = new LinkedHashSet<>();
        private final Set<Occurrence> occurrences = new LinkedHashSet<>();

        private ProblemType as(String type) {
            List<Occurrence> sorted = new ArrayList<>(occurrences);
            sorted.sort(Comparator.comparing(Occurrence::specification)
                    .thenComparing(Occurrence::path)
                    .thenComparing(
                            occurrence -> METHODS.indexOf(occurrence.method().toLowerCase(Locale.ROOT))));
            return new ProblemType(
                    type.substring(type.lastIndexOf('/') + 1),
                    type,
                    statuses.iterator().next(),
                    titles.iterator().next(),
                    List.copyOf(examples),
                    List.copyOf(sorted));
        }
    }

    /**
     * The object itself, or the one that it refers to inside the same specification. vacuum resolves the references
     * to other files, and it can leave one that stays inside the document.
     */
    private static Map<?, ?> resolved(Map<?, ?> specification, Object value) {
        Map<?, ?> object = map(value);
        // A reference can lead to another one. No chain in a specification is this long.
        for (int step = 0; step < 16 && object.get("$ref") instanceof String reference; step++) {
            if (!reference.startsWith("#/")) {
                throw new IllegalArgumentException(
                        "A bundled specification still refers to another file: " + reference);
            }
            Object target = specification;
            for (String key : reference.substring(2).split("/")) {
                // The two escapes of a JSON Pointer.
                target = map(target).get(key.replace("~1", "/").replace("~0", "~"));
            }
            object = map(target);
        }
        return object;
    }

    private static Map<?, ?> map(Object value) {
        return value instanceof Map<?, ?> map ? map : Map.of();
    }

    private static String text(Object value) {
        return value instanceof String text ? text.strip() : "";
    }
}
