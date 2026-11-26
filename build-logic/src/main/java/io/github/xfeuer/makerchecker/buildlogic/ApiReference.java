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
import org.gradle.api.file.ConfigurableFileTree;
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
 * Renders the API reference with vacuum: static pages for every specification, with a catalog in front. See ADR
 * 0015.
 *
 * <p>vacuum renders every specification that it finds in a directory. So the task copies the contracts into a
 * directory of their own first, and nothing else gets into the reference.
 *
 * <p>The task changes one thing about what vacuum wrote: it lets the pages of the catalog scroll.
 *
 * <p>The reference loads nothing from another host. The task fails when one of its pages would.
 */
@DisableCachingByDefault(because = "vacuum does not write the same bytes twice")
public abstract class ApiReference extends DefaultTask {

    /** vacuum keeps what it knows about its last run in this file. It is no part of the reference. */
    private static final String STATE_OF_VACUUM = ".printingpress-state.db";

    /** The unpacked archive of vacuum. */
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getVacuumHome();

    /** The name of the program in that directory. */
    @Input
    public abstract Property<String> getExecutable();

    /** The title of the catalog. */
    @Input
    public abstract Property<String> getCatalogTitle();

    /**
     * The specifications and every file that they refer to. Each file keeps its path from the root of this tree, so
     * a reference from one file to another still holds in the copy.
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileTree getContracts();

    /** The reference. Its first page is {@code index.html}. */
    @OutputDirectory
    public abstract DirectoryProperty getReference();

    @Inject
    protected abstract ExecOperations getExecOperations();

    @Inject
    protected abstract FileSystemOperations getFiles();

    @TaskAction
    void render() throws IOException {
        if (getContracts().isEmpty()) {
            // A reference of nothing would pass for the wrong reason.
            throw new GradleException("There is no specification to render under %s."
                    .formatted(getContracts().getDir()));
        }
        File contracts = new File(getTemporaryDir(), "contracts");
        getFiles().sync(copy -> {
            copy.from(getContracts());
            copy.into(contracts);
        });
        // vacuum adds to a directory that it finds. A page of a specification that is gone must not stay.
        File reference = getReference().get().getAsFile();
        getFiles().delete(old -> old.delete(reference));

        ReferenceCommand command = new ReferenceCommand(
                new File(getVacuumHome().get().getAsFile(), getExecutable().get()).getAbsolutePath(),
                getCatalogTitle().get(),
                reference.getAbsolutePath());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ExecResult run = getExecOperations().exec(vacuum -> {
            vacuum.commandLine(command.arguments());
            vacuum.workingDir(contracts);
            // vacuum reports its progress on both streams. Only a failure shows it.
            vacuum.setStandardOutput(output);
            vacuum.setErrorOutput(output);
            vacuum.setIgnoreExitValue(true);
        });

        Path index = reference.toPath().resolve("index.html");
        if (run.getExitValue() != 0 || !Files.isRegularFile(index)) {
            throw new GradleException(
                    "vacuum could not render the API reference. It ended with exit code %d and wrote:\n%s"
                            .formatted(
                                    run.getExitValue(),
                                    VacuumOutput.readable(output.toString(StandardCharsets.UTF_8))));
        }
        Files.deleteIfExists(reference.toPath().resolve(STATE_OF_VACUUM));
        ScrollingCatalog.in(reference.toPath());

        List<String> outside = OutsideReferences.under(reference.toPath());
        if (!outside.isEmpty()) {
            throw new VerificationException(
                    "The API reference must not load anything from another host, but it holds:\n  "
                            + String.join("\n  ", outside));
        }
        getLogger().lifecycle("API reference: {}", index.toUri());
    }
}
