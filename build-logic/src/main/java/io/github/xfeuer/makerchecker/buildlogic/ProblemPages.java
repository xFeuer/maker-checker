package io.github.xfeuer.makerchecker.buildlogic;

import groovy.json.JsonSlurper;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.VerificationException;
import org.gradle.process.ExecOperations;
import org.gradle.process.ExecResult;
import org.gradle.work.DisableCachingByDefault;

/**
 * Writes a page for each problem type that the specifications name, and a page that lists them. See ADR 0010 and
 * ADR 0015.
 *
 * <p>The type of a problem is an address, and the page of the type is what that address serves. The specifications
 * are the only source of the pages: vacuum bundles each one into a JSON document, and the task reads the examples
 * of the error responses from it.
 *
 * <p>The task fails when the specifications do not agree on a type, such as two titles for one type.
 */
@DisableCachingByDefault(because = "The pages are quick to write")
public abstract class ProblemPages extends DefaultTask {

    /** The unpacked archive of vacuum. */
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getVacuumHome();

    /** The name of the program in that directory. */
    @Input
    public abstract Property<String> getExecutable();

    /** The title of the API reference, which every page links to. */
    @Input
    public abstract Property<String> getReferenceTitle();

    /** The specifications to read. */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSpecifications();

    /** Every file that a specification can refer to. The task runs again when one of them changes. */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSources();

    /** The pages: {@code index.html}, and one file for each type, named after it. */
    @OutputDirectory
    public abstract DirectoryProperty getPages();

    @Inject
    protected abstract ExecOperations getExecOperations();

    @Inject
    protected abstract FileSystemOperations getFiles();

    @TaskAction
    void write() throws IOException {
        List<File> specifications = getSpecifications().getFiles().stream()
                .sorted(Comparator.comparing(File::getAbsolutePath))
                .toList();
        if (specifications.isEmpty()) {
            // Pages of nothing would pass for the wrong reason.
            throw new GradleException("There is no specification to read the problem types from.");
        }

        List<Object> bundled = new ArrayList<>();
        for (File specification : specifications) {
            bundled.add(new JsonSlurper().parse(bundle(specification, bundled.size()), StandardCharsets.UTF_8.name()));
        }
        List<ProblemType> problems;
        try {
            problems = ProblemCatalog.in(bundled);
        } catch (IllegalArgumentException e) {
            throw new VerificationException("The specifications do not agree on their problem types:\n  "
                    + e.getMessage().replace("\n", "\n  "));
        }

        // A page of a type that is gone must not stay.
        Path pages = getPages().get().getAsFile().toPath();
        getFiles().delete(old -> old.delete(pages.toFile()));
        Files.createDirectories(pages);
        ProblemHtml html = new ProblemHtml(getReferenceTitle().get());
        for (ProblemType problem : problems) {
            Files.writeString(pages.resolve(ProblemHtml.fileName(problem)), html.page(problem));
        }
        Path index = pages.resolve("index.html");
        Files.writeString(index, html.index(problems));

        List<String> outside = OutsideReferences.under(pages);
        if (!outside.isEmpty()) {
            throw new VerificationException(
                    "The pages of the problem types must not load anything from another host, but they hold:\n  "
                            + String.join("\n  ", outside));
        }
        getLogger().lifecycle("Problem types: {}", index.toUri());
    }

    /** vacuum bundles the specification into a file of its own, in the temporary directory of the task. */
    private File bundle(File specification, int number) {
        File bundle = new File(getTemporaryDir(), "bundle-%d.json".formatted(number));
        BundleCommand command = new BundleCommand(
                new File(getVacuumHome().get().getAsFile(), getExecutable().get()).getAbsolutePath(),
                specification.getName(),
                bundle.getAbsolutePath());
        // A bundle of an earlier run must not stand in for one that vacuum could not write.
        getFiles().delete(old -> old.delete(bundle));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ExecResult run = getExecOperations().exec(vacuum -> {
            vacuum.commandLine(command.arguments());
            vacuum.workingDir(specification.getParentFile());
            // Only a failure shows what vacuum wrote.
            vacuum.setStandardOutput(output);
            vacuum.setErrorOutput(output);
            vacuum.setIgnoreExitValue(true);
        });
        if (run.getExitValue() != 0 || !bundle.isFile()) {
            throw new GradleException("vacuum could not bundle %s. It ended with exit code %d and wrote:\n%s"
                    .formatted(
                            specification,
                            run.getExitValue(),
                            VacuumOutput.readable(output.toString(StandardCharsets.UTF_8))));
        }
        return bundle;
    }
}
