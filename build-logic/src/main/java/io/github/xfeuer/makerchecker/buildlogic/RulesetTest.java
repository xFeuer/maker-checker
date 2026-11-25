package io.github.xfeuer.makerchecker.buildlogic;

import groovy.json.JsonException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.VerificationException;
import org.gradle.process.ExecOperations;
import org.gradle.process.ExecResult;
import org.gradle.work.DisableCachingByDefault;

/**
 * Tests the rules of a ruleset with vacuum. See ADR 0009.
 *
 * <p>A suite names documents and says what vacuum has to report for each. A case passes when vacuum reports exactly
 * that. So a rule that stops finding what it is for fails the build, and so does a rule that starts finding more.
 */
@DisableCachingByDefault(because = "The tests are quick")
public abstract class RulesetTest extends DefaultTask {

    /** The unpacked archive of vacuum. */
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getVacuumHome();

    /** The name of the program in that directory. */
    @Input
    public abstract Property<String> getExecutable();

    /** The suite, in the format of {@code vacuum test}. */
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getSuite();

    /** Every file that the suite can reach: the ruleset, the documents and what they refer to. */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSources();

    /** vacuum runs in this directory, so a failure names its document by the path from here. */
    @Internal
    public abstract DirectoryProperty getWorkingDirectory();

    /** The result of the last run, as vacuum writes it. */
    @OutputFile
    public abstract RegularFileProperty getResult();

    @Inject
    protected abstract ExecOperations getExecOperations();

    @TaskAction
    void test() throws IOException {
        Path directory = getWorkingDirectory().get().getAsFile().toPath();
        File program =
                new File(getVacuumHome().get().getAsFile(), getExecutable().get());
        String suite = directory
                .relativize(getSuite().get().getAsFile().toPath())
                .toString()
                .replace(File.separatorChar, '/');

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        ExecResult run = getExecOperations().exec(vacuum -> {
            vacuum.commandLine(program.getAbsolutePath(), "test", "--no-update-check", suite);
            vacuum.workingDir(directory.toFile());
            vacuum.setStandardOutput(output);
            vacuum.setErrorOutput(errors);
            // The result decides, and the message below shows it.
            vacuum.setIgnoreExitValue(true);
        });

        String result = output.toString(StandardCharsets.UTF_8);
        Files.writeString(getResult().get().getAsFile().toPath(), result);
        List<String> problems = problemsIn(result);
        if (run.getExitValue() == 0 && problems.isEmpty()) {
            return;
        }
        if (problems.isEmpty()) {
            throw new VerificationException(
                    "vacuum could not test the ruleset. It ended with exit code %d and wrote:\n  %s"
                            .formatted(
                                    run.getExitValue(),
                                    errors.toString(StandardCharsets.UTF_8).strip()));
        }
        throw new VerificationException("The tests of the ruleset do not pass:\n  " + String.join("\n  ", problems));
    }

    /** vacuum writes no result when it cannot read the suite. Then its exit code and its messages have to do. */
    private static List<String> problemsIn(String result) {
        try {
            return RulesetTestResult.problems(result);
        } catch (JsonException e) {
            return List.of();
        }
    }
}
