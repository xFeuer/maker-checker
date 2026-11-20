package io.github.xfeuer.makerchecker.buildlogic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileTree;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.IgnoreEmptyDirectories;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.VerificationException;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.process.ExecOperations;
import org.gradle.work.DisableCachingByDefault;

/**
 * Generates the Allure report from the results that the test tasks of a project wrote. See ADR 0007.
 *
 * <p>Allure Report 2 is a Java program. The task runs it on a Java toolchain, so it needs no start script, no
 * {@code JAVA_HOME} and nothing on the path.
 *
 * <p>The report is one HTML file that loads nothing from another host. The task fails when it would.
 */
@DisableCachingByDefault(because = "The report is quick to generate")
public abstract class AllureReport extends DefaultTask {

    /** The unpacked distribution of Allure Report 2. */
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getAllureHome();

    /** Holds one directory of results for each test task. */
    @Internal
    public abstract DirectoryProperty getResultsDirectory();

    /** The task has nothing to do as long as no test task wrote a result. */
    @InputFiles
    @SkipWhenEmpty
    @IgnoreEmptyDirectories
    @PathSensitive(PathSensitivity.RELATIVE)
    public FileTree getResults() {
        return getResultsDirectory().getAsFileTree();
    }

    @Input
    public abstract Property<String> getReportName();

    @OutputDirectory
    public abstract DirectoryProperty getReportDirectory();

    @Nested
    public abstract Property<JavaLauncher> getLauncher();

    @Inject
    protected abstract ExecOperations getExecOperations();

    @Inject
    protected abstract ObjectFactory getObjects();

    @TaskAction
    void generate() throws IOException {
        File home = getAllureHome().get().getAsFile();
        File report = getReportDirectory().get().getAsFile();
        List<String> arguments = new ArrayList<>(List.of("generate"));
        resultsOfEachTestTask().forEach(results -> arguments.add(results.getAbsolutePath()));
        arguments.addAll(List.of("--output", report.getAbsolutePath(), "--clean", "--single-file"));
        arguments.addAll(List.of("--report-name", getReportName().get()));

        getExecOperations().javaexec(allure -> {
            allure.setExecutable(getLauncher().get().getExecutablePath().getAsFile());
            // Allure finds its configuration and its plugins next to the directory of its jars.
            allure.classpath(getObjects().fileTree().from(new File(home, "lib")).include("*.jar"));
            allure.classpath(new File(home, "lib/config"));
            allure.getMainClass().set("io.qameta.allure.CommandLine");
            allure.args(arguments);
            // By default Allure puts a Google Analytics script into the report and sends usage statistics while
            // it generates. This turns both off.
            allure.environment("ALLURE_NO_ANALYTICS", "true");
        });

        Path index = report.toPath().resolve("index.html");
        List<String> outside = OutsideReferences.in(Files.readString(index));
        if (!outside.isEmpty()) {
            throw new VerificationException(
                    "The Allure report must not load anything from another host, but it holds:\n  "
                            + String.join("\n  ", outside));
        }
        getLogger().lifecycle("Allure report: {}", index.toUri());
    }

    private List<File> resultsOfEachTestTask() {
        File[] directories = getResultsDirectory().get().getAsFile().listFiles(File::isDirectory);
        return Arrays.stream(directories).sorted().toList();
    }
}
