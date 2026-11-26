package io.github.xfeuer.makerchecker.buildlogic;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.VerificationException;
import org.gradle.process.ExecOperations;
import org.gradle.process.ExecResult;
import org.gradle.work.DisableCachingByDefault;

/**
 * Lints OpenAPI specifications with vacuum. See ADR 0009.
 *
 * <p>A specification is clean when vacuum reports nothing, at any level. The task fails when vacuum has a finding,
 * and it lists them all.
 */
@DisableCachingByDefault(because = "The lint is quick")
public abstract class SpecificationLint extends DefaultTask {

    /** The unpacked archive of vacuum. */
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getVacuumHome();

    /** The name of the program in that directory. */
    @Input
    public abstract Property<String> getExecutable();

    /** The rules, in the format of Spectral. */
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getRuleset();

    /**
     * The functions of our own that a rule of the ruleset can name: a directory of JavaScript files, which vacuum
     * loads. A project whose rules need no such function sets none.
     */
    @Optional
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getFunctions();

    /** The specifications to lint. */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSpecifications();

    /** Every file that a specification can refer to. The task runs again when one of them changes. */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSources();

    /** vacuum runs in this directory, so a finding names its specification by the path from here. */
    @Internal
    public abstract DirectoryProperty getWorkingDirectory();

    /** The findings of the last run, one on each line. The file is empty when the specifications are clean. */
    @OutputFile
    public abstract RegularFileProperty getFindings();

    @Inject
    protected abstract ExecOperations getExecOperations();

    @TaskAction
    void lint() throws IOException {
        Path directory = getWorkingDirectory().get().getAsFile().toPath();
        List<String> specifications = getSpecifications().getFiles().stream()
                .map(specification -> pathFrom(directory, specification))
                .sorted()
                .toList();
        if (specifications.isEmpty()) {
            // A lint that finds no specification would pass for the wrong reason.
            throw new GradleException("There is no specification to lint under %s.".formatted(directory));
        }

        LintCommand command = new LintCommand(
                new File(getVacuumHome().get().getAsFile(), getExecutable().get()).getAbsolutePath(),
                pathFrom(directory, getRuleset().get().getAsFile()),
                java.util.Optional.ofNullable(getFunctions().getAsFile().getOrNull())
                        .map(functions -> pathFrom(directory, functions)),
                specifications);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        ExecResult result = getExecOperations().exec(vacuum -> {
            vacuum.commandLine(command.arguments());
            vacuum.workingDir(directory.toFile());
            vacuum.setStandardOutput(output);
            vacuum.setErrorOutput(errors);
            // The findings decide, and the message below shows them.
            vacuum.setIgnoreExitValue(true);
        });

        VacuumRun run = new VacuumRun(
                result.getExitValue(),
                output.toString(StandardCharsets.UTF_8),
                errors.toString(StandardCharsets.UTF_8));
        Files.write(getFindings().get().getAsFile().toPath(), run.findings());
        run.failure().ifPresent(message -> {
            throw new VerificationException(message);
        });
    }

    /** The path as vacuum gets it and prints it: from the directory, with forward slashes on every platform. */
    private static String pathFrom(Path directory, File file) {
        return directory.relativize(file.toPath()).toString().replace(File.separatorChar, '/');
    }
}
