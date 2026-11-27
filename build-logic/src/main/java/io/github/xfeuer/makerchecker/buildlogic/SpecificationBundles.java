package io.github.xfeuer.makerchecker.buildlogic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
import org.gradle.process.ExecOperations;
import org.gradle.work.DisableCachingByDefault;

/**
 * Bundles every specification into a JSON document of its own, which refers to no other file. See ADR 0019.
 *
 * <p>An application carries the bundle of its specification, and its tests hold it to that document. The bundle of
 * {@code account-service/openapi.yaml} is {@code account-service/openapi.json}.
 */
@DisableCachingByDefault(because = "A bundle is quick to write")
public abstract class SpecificationBundles extends DefaultTask {

    /** The unpacked archive of vacuum. */
    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getVacuumHome();

    /** The name of the program in the archive. */
    @Input
    public abstract Property<String> getExecutable();

    /** The specifications to bundle. Each lies in the directory of its application. */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSpecifications();

    /** Every file that a specification can refer to. A change of one changes a bundle. */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSources();

    @OutputDirectory
    public abstract DirectoryProperty getBundles();

    @Inject
    protected abstract ExecOperations getExecOperations();

    @Inject
    protected abstract FileSystemOperations getFiles();

    @TaskAction
    void bundle() throws IOException {
        List<File> specifications = getSpecifications().getFiles().stream()
                .sorted(Comparator.comparing(File::getAbsolutePath))
                .toList();
        if (specifications.isEmpty()) {
            // An application would then start without its contract, and say so much later.
            throw new GradleException("There is no specification to bundle.");
        }
        // The bundle of a specification that is gone must not stay.
        File bundles = getBundles().get().getAsFile();
        getFiles().delete(old -> old.delete(bundles));
        File program =
                new File(getVacuumHome().get().getAsFile(), getExecutable().get());
        for (File specification : specifications) {
            File bundle = new File(bundles, placeOf(specification));
            Files.createDirectories(bundle.getParentFile().toPath());
            Bundling.bundle(getExecOperations(), program, specification, bundle);
        }
    }

    /** Where a bundle lies under the output directory: in the directory of its application, by the name of the file. */
    static String placeOf(File specification) {
        String name = specification.getName();
        return specification.getParentFile().getName() + "/" + name.substring(0, name.lastIndexOf('.')) + ".json";
    }
}
