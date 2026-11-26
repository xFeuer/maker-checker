package io.github.xfeuer.makerchecker.buildlogic;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LintCommandTest {

    private static final String PROGRAM = "/build/vacuum/vacuum";

    @Test
    void lintsEverySpecificationWithTheRulesetAndFailsOnAFindingOfAnyLevel() {
        LintCommand command =
                new LintCommand(PROGRAM, "ruleset.yaml", Optional.empty(), List.of("a/hooks.yaml", "a/openapi.yaml"));

        assertThat(command.arguments())
                .containsExactly(
                        PROGRAM,
                        "lint",
                        "--format",
                        "text",
                        "--no-update-check",
                        "--no-banner",
                        "--no-style",
                        "--fail-severity",
                        "hint",
                        "--ruleset",
                        "ruleset.yaml",
                        "a/hooks.yaml",
                        "a/openapi.yaml");
    }

    @Test
    void namesTheFunctionsOfTheProjectWhenItHasSome() {
        LintCommand command =
                new LintCommand(PROGRAM, "ruleset.yaml", Optional.of("functions"), List.of("a/openapi.yaml"));

        assertThat(command.arguments())
                .containsSubsequence("--ruleset", "ruleset.yaml", "--functions", "functions", "a/openapi.yaml")
                .endsWith("a/openapi.yaml");
    }

    @Test
    void namesNoFunctionsWhenTheProjectHasNone() {
        LintCommand command = new LintCommand(PROGRAM, "ruleset.yaml", Optional.empty(), List.of("a/openapi.yaml"));

        assertThat(command.arguments()).doesNotContain("--functions");
    }
}
