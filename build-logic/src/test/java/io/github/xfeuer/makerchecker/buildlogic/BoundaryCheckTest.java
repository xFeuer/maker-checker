package io.github.xfeuer.makerchecker.buildlogic;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Runs the boundary check in real builds: each test writes a small build and runs its {@code check} task. */
class BoundaryCheckTest {

    @TempDir
    Path fixture;

    @Test
    void passesWhenTheAutomationKeepsToItself() throws IOException {
        checkedProject(":automation:api");
        checkedProject(":automation:suites", ":automation:api");

        BuildResult result = check().build();

        assertThat(result.task(":automation:suites:checkBoundary"))
                .extracting(BuildTask::getOutcome)
                .isEqualTo(TaskOutcome.SUCCESS);
    }

    @Test
    void failsWhenAnAutomationProjectDependsOnThePlatform() throws IOException {
        project(":platform:account-service");
        checkedProject(":automation:suites", ":platform:account-service");

        BuildResult result = check().buildAndFail();

        assertThat(result.task(":automation:suites:checkBoundary"))
                .extracting(BuildTask::getOutcome)
                .isEqualTo(TaskOutcome.FAILED);
        assertThat(result.getOutput())
                .contains("The automation must not depend on the code of the applications.")
                .contains(":automation:suites -> :platform:account-service")
                .contains("in compileClasspath, runtimeClasspath, testCompileClasspath, testRuntimeClasspath");
    }

    @Test
    void failsWhenThePlatformArrivesThroughAnotherProject() throws IOException {
        project(":platform:account-service");
        project(":shared:model", ":platform:account-service");
        checkedProject(":automation:suites", ":shared:model");

        BuildResult result = check().buildAndFail();

        assertThat(result.getOutput()).contains(":automation:suites -> :shared:model -> :platform:account-service");
    }

    @Test
    void failsWhenAnAutomationProjectDependsOnTheChassis() throws IOException {
        project(":chassis");
        checkedProject(":automation:suites", ":chassis");

        BuildResult result = check().buildAndFail();

        assertThat(result.task(":automation:suites:checkBoundary"))
                .extracting(BuildTask::getOutcome)
                .isEqualTo(TaskOutcome.FAILED);
        assertThat(result.getOutput()).contains(":automation:suites -> :chassis");
    }

    private GradleRunner check() {
        // The repository builds with the configuration cache on, so the check has to work with it.
        return GradleRunner.create()
                .withProjectDir(fixture.toFile())
                .withPluginClasspath()
                .withArguments("check", "--configuration-cache");
    }

    /** Adds a Java library that applies the plugin under test. It depends on the projects at the given paths. */
    private void checkedProject(String path, String... dependencies) throws IOException {
        javaLibrary(path, "id(\"makerchecker.boundary-conventions\")", dependencies);
    }

    /** Adds a Java library that the check does not look at. It depends on the projects at the given paths. */
    private void project(String path, String... dependencies) throws IOException {
        javaLibrary(path, "", dependencies);
    }

    private void javaLibrary(String path, String plugin, String... dependencies) throws IOException {
        Files.writeString(
                fixture.resolve("settings.gradle.kts"),
                "include(\"%s\")\n".formatted(path),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
        String declarations = Arrays.stream(dependencies)
                .map("    implementation(project(\"%s\"))"::formatted)
                .collect(Collectors.joining("\n"));
        String script = """
                plugins {
                    `java-library`
                    %s
                }

                dependencies {
                %s
                }
                """.formatted(plugin, declarations);
        // The project ":automation:suites" lives in the directory automation/suites.
        Path directory = fixture.resolve(path.substring(1).replace(':', '/'));
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("build.gradle.kts"), script);
    }
}
