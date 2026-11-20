package io.github.xfeuer.makerchecker.buildlogic;

import java.util.List;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.process.CommandLineArgumentProvider;

/**
 * Tells the Allure adapter in the JVM of a test task where to write its results.
 *
 * <p>The directory is an output of the task, and its path stays out of the inputs. So the path can differ from one
 * machine to the next without making the task look changed.
 */
public abstract class AllureResults implements CommandLineArgumentProvider {

    @OutputDirectory
    public abstract DirectoryProperty getDirectory();

    @Override
    public Iterable<String> asArguments() {
        return List.of(
                "-Dallure.results.directory=" + getDirectory().get().getAsFile().getAbsolutePath());
    }
}
