package io.github.xfeuer.makerchecker.buildlogic;

import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Makes readable what vacuum writes for a terminal. */
final class VacuumOutput {

    // A color or a style, as a terminal takes it.
    private static final Pattern STYLE = Pattern.compile("\u001B\\[[0-9;]*m");

    private VacuumOutput() {}

    /**
     * The text with one indented line for each message. vacuum overwrites its progress in place, with a carriage
     * return, and it colors its messages.
     */
    static String readable(String output) {
        String messages = STYLE.matcher(output)
                .replaceAll("")
                .replace('\r', '\n')
                .lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.joining("\n  "));
        return "  " + (messages.isEmpty() ? "(nothing)" : messages);
    }
}
