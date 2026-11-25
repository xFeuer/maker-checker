package io.github.xfeuer.makerchecker.buildlogic;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Runs the plugin in real builds, against a release of vacuum that the tests make up.
 *
 * <p>The archive of that release holds a text file under the name of the program, so no test here can run it. The
 * tests cover the way from a release to the unpacked program: the repository, the archive of this platform and its
 * checksum.
 */
class ContractConventionsTest {

    private static final String VERSION = "1.2.3";
    private static final VacuumPlatform PLATFORM =
            VacuumPlatform.of(System.getProperty("os.name"), System.getProperty("os.arch"));
    private static final String ARCHIVE = "vacuum_%s_%s.tar.gz".formatted(VERSION, PLATFORM.classifier());
    private static final String PROGRAM = "Not a program.\n";

    @TempDir
    static Path release;

    @TempDir
    Path fixture;

    /** The archive as the repository of the fixture serves it. */
    private Path served;

    private Path checksums;

    /** Gradle packs the archive, so the tests hold no code for the tar format. */
    @BeforeAll
    static void packsTheArchiveOfTheRelease() throws IOException {
        Files.writeString(release.resolve("settings.gradle.kts"), "");
        Files.writeString(release.resolve("build.gradle.kts"), """
                tasks.register<Tar>("pack") {
                    from("content")
                    compression = Compression.GZIP
                    archiveFileName = "%s"
                    destinationDirectory = layout.projectDirectory
                }
                """.formatted(ARCHIVE));
        Files.createDirectory(release.resolve("content"));
        Files.writeString(release.resolve("content").resolve(PLATFORM.executable()), PROGRAM);

        GradleRunner.create()
                .withProjectDir(release.toFile())
                .withArguments("pack")
                .build();
    }

    @BeforeEach
    void writesABuildThatAppliesThePlugin() throws IOException {
        // The repository has the layout of the GitHub releases of vacuum, as in the settings of this repository.
        Path releases = fixture.resolve("releases");
        Files.writeString(fixture.resolve("settings.gradle.kts"), """
                dependencyResolutionManagement {
                    repositories {
                        ivy {
                            url = uri("%s")
                            patternLayout { artifact("v[revision]/[module]_[revision]_[classifier].[ext]") }
                            metadataSources { artifact() }
                        }
                    }
                    versionCatalogs {
                        create("libs") {
                            version("vacuum", "%s")
                        }
                    }
                }
                """.formatted(releases.toUri(), VERSION));
        Files.writeString(fixture.resolve("build.gradle.kts"), """
                plugins {
                    id("makerchecker.contract-conventions")
                }
                """);

        served = releases.resolve("v" + VERSION).resolve(ARCHIVE);
        Files.createDirectories(served.getParent());
        Files.copy(release.resolve(ARCHIVE), served);

        checksums = fixture.resolve("gradle/vacuum-checksums.txt");
        Files.createDirectories(checksums.getParent());
        Files.writeString(checksums, "%s  %s\n".formatted(Sha256.of(served), ARCHIVE));
    }

    @Test
    void unpacksTheArchiveOfThisPlatformWhenItMatchesItsChecksum() throws IOException {
        BuildResult result = unpackVacuum().build();

        assertThat(result.task(":unpackVacuum"))
                .extracting(BuildTask::getOutcome)
                .isEqualTo(TaskOutcome.SUCCESS);
        assertThat(fixture.resolve("build/vacuum").resolve(PLATFORM.executable()))
                .hasContent(PROGRAM);
    }

    @Test
    void unpacksNothingWhenTheArchiveIsNotTheOneOfTheChecksumsFile() throws IOException {
        String stated = Sha256.of(served);
        Files.write(served, new byte[] {0}, StandardOpenOption.APPEND);

        BuildResult result = unpackVacuum().buildAndFail();

        assertThat(result.task(":unpackVacuum"))
                .extracting(BuildTask::getOutcome)
                .isEqualTo(TaskOutcome.FAILED);
        assertThat(result.getOutput())
                .contains("The archive %s does not match its checksum. Nothing was unpacked.".formatted(ARCHIVE))
                .contains("SHA-256 of the archive: " + Sha256.of(served))
                .contains(stated);
        assertThat(fixture.resolve("build/vacuum")).isEmptyDirectory();
    }

    @Test
    void failsWhenTheChecksumsFileIsTheOneOfAnotherRelease() throws IOException {
        Files.writeString(checksums, "%s  %s\n".formatted(Sha256.of(served), ARCHIVE.replace(VERSION, "1.2.4")));

        BuildResult result = unpackVacuum().buildAndFail();

        assertThat(result.getOutput()).contains("states no SHA-256 for %s.".formatted(ARCHIVE));
    }

    private GradleRunner unpackVacuum() {
        // The repository builds with the configuration cache on, so the plugin has to work with it.
        return GradleRunner.create()
                .withProjectDir(fixture.toFile())
                .withPluginClasspath()
                .withArguments("unpackVacuum", "--configuration-cache");
    }
}
