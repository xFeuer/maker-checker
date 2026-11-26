package io.github.xfeuer.makerchecker.buildlogic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReferenceCommandTest {

    @Test
    void rendersTheContractsOfItsDirectoryAsHtmlAndNothingElse() {
        ReferenceCommand command =
                new ReferenceCommand("/build/vacuum/vacuum", "maker-checker API reference", "/build/site/api");

        assertThat(command.arguments())
                .containsExactly(
                        "/build/vacuum/vacuum",
                        "docs",
                        "--no-update-check",
                        "--no-logo",
                        "--no-diagnostics",
                        "--no-llm",
                        "--no-json",
                        "--no-footer",
                        "--catalog-title",
                        "maker-checker API reference",
                        "--output",
                        "/build/site/api",
                        ".");
    }
}
