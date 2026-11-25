package io.github.xfeuer.makerchecker.buildlogic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ArchiveOperations;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

/**
 * Unpacks the archive of a program that the build runs, once the archive matches its checksum. See ADR 0009.
 *
 * <p>Gradle downloads the archive from a release page. The checksums file of that release lies in the repository,
 * so the build notices an archive that is not the one the file was written for. The task unpacks nothing until the
 * two match.
 */
@DisableCachingByDefault(because = "Unpacking is as quick as a cache")
public abstract class VerifiedUnpack extends DefaultTask {

    /** The archive, a tar file compressed with gzip. Gradle resolves it, so the collection holds one file. */
    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getArchive();

    /** The name of the archive in the checksums file. Gradle keeps the archive under a name of its own. */
    @Input
    public abstract Property<String> getArchiveName();

    /** The checksums file of the release: each line holds the SHA-256 of a file and its name. */
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getChecksums();

    @OutputDirectory
    public abstract DirectoryProperty getDestination();

    @Inject
    protected abstract ArchiveOperations getArchiveOperations();

    @Inject
    protected abstract FileSystemOperations getFileSystemOperations();

    @TaskAction
    void unpack() throws IOException {
        File archive = getArchive().getSingleFile();
        String name = getArchiveName().get();
        File checksums = getChecksums().get().getAsFile();

        String stated = Sha256.statedFor(name, Files.readString(checksums.toPath()))
                .orElseThrow(() -> new GradleException(("%s states no SHA-256 for %s."
                                + " It has to be the checksums file of the release that the archive comes from.")
                        .formatted(checksums, name)));
        String actual = Sha256.of(archive.toPath());
        if (!actual.equals(stated)) {
            throw new GradleException("""
                    The archive %s does not match its checksum. Nothing was unpacked.
                      SHA-256 of the archive: %s
                      SHA-256 in %s: %s""".formatted(name, actual, checksums, stated));
        }

        getFileSystemOperations().sync(copy -> {
            copy.from(getArchiveOperations().tarTree(getArchiveOperations().gzip(archive)));
            copy.into(getDestination());
        });
    }
}
