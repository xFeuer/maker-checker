package io.github.xfeuer.makerchecker.buildlogic;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.process.CommandLineArgumentProvider;

/**
 * Tells the component tests of a service which image their database runs.
 *
 * <p>The stack and the component tests run the same PostgreSQL, and the Compose file of the stack is the one place
 * that names it. See ADR 0016. The image is an input of the test task, so another image is another test run.
 */
public abstract class DatabaseImage implements CommandLineArgumentProvider {

    /** The system property that the tests read. */
    static final String PROPERTY = "makerchecker.database.image";

    /** The image line of a service, such as {@code image: postgres:18.7-alpine}, with or without quotes. */
    private static final Pattern IMAGE_LINE = Pattern.compile(
            "^[ \\t]+image:[ \\t]*[\"']?(postgres:[^\\s\"'#]+)[\"']?[ \\t]*(?:#.*)?$", Pattern.MULTILINE);

    @Input
    public abstract Property<String> getImage();

    @Override
    public Iterable<String> asArguments() {
        return List.of("-D" + PROPERTY + "=" + getImage().get());
    }

    /**
     * Finds the PostgreSQL image in the text of a Compose file. Every database of the stack has to run the same one.
     */
    public static String namedIn(String composeFile) {
        Set<String> images = new TreeSet<>();
        Matcher line = IMAGE_LINE.matcher(composeFile);
        while (line.find()) {
            images.add(line.group(1));
        }
        if (images.size() != 1) {
            throw new InvalidUserDataException(
                    "The Compose file of the stack has to name one PostgreSQL image, with its tag, but it names "
                            + (images.isEmpty() ? "none" : String.join(" and ", images))
                            + ". See docs/adr/0016-a-postgresql-database-for-each-service.md.");
        }
        return images.iterator().next();
    }
}
