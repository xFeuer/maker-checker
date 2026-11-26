package io.github.xfeuer.makerchecker.buildlogic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The command line that lints specifications with vacuum.
 *
 * <p>vacuum gets every path as the path from the directory that it runs in, with forward slashes. A project can have
 * functions of its own for its rules, as JavaScript files in one directory. vacuum loads them only when the command
 * names that directory, and it cannot lint with a rule whose function it did not load.
 */
record LintCommand(String program, String ruleset, Optional<String> functions, List<String> specifications) {

    /** The program and its arguments, in the order in which the process gets them. */
    List<String> arguments() {
        List<String> arguments = new ArrayList<>();
        arguments.add(program);
        // The text format has one line for each finding and nothing else. The other flags keep vacuum from
        // asking GitHub for a newer release and from decorating its output.
        arguments.addAll(List.of("lint", "--format", "text", "--no-update-check", "--no-banner", "--no-style"));
        // A finding of any level fails the run. The lowest level is the hint.
        arguments.addAll(List.of("--fail-severity", "hint"));
        arguments.addAll(List.of("--ruleset", ruleset));
        functions.ifPresent(directory -> arguments.addAll(List.of("--functions", directory)));
        arguments.addAll(specifications);
        return List.copyOf(arguments);
    }
}
