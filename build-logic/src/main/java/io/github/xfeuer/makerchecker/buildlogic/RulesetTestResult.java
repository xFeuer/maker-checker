package io.github.xfeuer.makerchecker.buildlogic;

import groovy.json.JsonSlurper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads the result that {@code vacuum test} writes, and says what did not pass.
 *
 * <p>The result is JSON. Each case of the suite has the failures of its expectations and the errors that kept it
 * from running.
 */
final class RulesetTestResult {

    private RulesetTestResult() {}

    /** One line for each failure and each error. The list is empty when every case passed. */
    static List<String> problems(String json) {
        Map<?, ?> result = map(new JsonSlurper().parseText(json));
        List<String> problems = new ArrayList<>();
        for (Object error : list(result.get("errors"))) {
            problems.add(String.valueOf(map(error).get("message")));
        }
        for (Object each : list(result.get("cases"))) {
            Map<?, ?> testCase = map(each);
            Object id = testCase.get("id");
            for (Object failure : list(testCase.get("failures"))) {
                problems.add(id + ": " + described(map(failure)));
            }
            for (Object error : list(testCase.get("errors"))) {
                problems.add(id + ": " + map(error).get("message"));
            }
        }
        return problems;
    }

    private static String described(Map<?, ?> failure) {
        Map<?, ?> expected = map(failure.get("expected"));
        Map<?, ?> actual = map(failure.get("actual"));
        if (actual.isEmpty() && !expected.isEmpty()) {
            return "vacuum did not report %s.".formatted(expected.get("rule"));
        }
        if (expected.isEmpty() && !actual.isEmpty()) {
            return "vacuum reported %s at %s:%s, which the case does not expect: %s"
                    .formatted(actual.get("rule"), actual.get("source"), actual.get("line"), actual.get("message"));
        }
        return "%s does not hold: %s".formatted(failure.get("assertion"), failure.get("code"));
    }

    private static Map<?, ?> map(Object value) {
        return value instanceof Map<?, ?> map ? map : Map.of();
    }

    private static List<?> list(Object value) {
        return value instanceof List<?> list ? list : List.of();
    }
}
