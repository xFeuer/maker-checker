package io.github.xfeuer.makerchecker.buildlogic;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * What a run of {@code vacuum lint} left behind: its exit code, and what it wrote to its two streams.
 *
 * <p>With the text format, vacuum writes each finding on a line of its standard output. It ends with 0 when it has
 * no finding, with 1 when it has some, and with another code when it could not lint.
 */
record VacuumRun(int exitCode, String output, String errors) {

    /** The findings, one in each element. */
    List<String> findings() {
        return lines(output).toList();
    }

    /** Says why the specifications are not clean. It is empty when they are. */
    Optional<String> failure() {
        List<String> findings = findings();
        if (exitCode == 0 && findings.isEmpty()) {
            return Optional.empty();
        }
        if ((exitCode == 0 || exitCode == 1) && !findings.isEmpty()) {
            String count = findings.size() == 1 ? "1 finding" : findings.size() + " findings";
            return Optional.of("The specifications do not lint clean. vacuum has %s:%s"
                    .formatted(count, indented(findings.stream())));
        }
        return Optional.of("vacuum could not lint the specifications. It ended with exit code %d and wrote:%s"
                .formatted(exitCode, indented(Stream.concat(lines(errors), lines(output)))));
    }

    private static Stream<String> lines(String text) {
        return text.lines().map(String::strip).filter(line -> !line.isEmpty());
    }

    private static String indented(Stream<String> lines) {
        return lines.map(line -> "\n  " + line).reduce("", String::concat);
    }
}
