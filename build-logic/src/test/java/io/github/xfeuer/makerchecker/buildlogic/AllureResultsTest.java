package io.github.xfeuer.makerchecker.buildlogic;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AllureResultsTest {

    @TempDir
    Path projectDirectory;

    @Test
    void pointsTheAllureAdapterAtTheDirectory() {
        Project project = ProjectBuilder.builder()
                .withProjectDir(projectDirectory.toFile())
                .build();
        AllureResults results = project.getObjects().newInstance(AllureResults.class);
        results.getDirectory().set(project.getLayout().getBuildDirectory().dir("allure-results/systemTest"));

        File expected = new File(project.getProjectDir(), "build/allure-results/systemTest");
        assertThat(results.asArguments()).containsExactly("-Dallure.results.directory=" + expected.getAbsolutePath());
    }
}
