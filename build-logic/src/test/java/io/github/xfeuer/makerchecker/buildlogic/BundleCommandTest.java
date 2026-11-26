package io.github.xfeuer.makerchecker.buildlogic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BundleCommandTest {

    @Test
    void bundlesOneSpecificationIntoOneFile() {
        BundleCommand command = new BundleCommand("/build/vacuum/vacuum", "openapi.yaml", "/build/tmp/bundle-0.json");

        assertThat(command.arguments())
                .containsExactly(
                        "/build/vacuum/vacuum",
                        "bundle",
                        "--no-update-check",
                        "--no-style",
                        "openapi.yaml",
                        "/build/tmp/bundle-0.json");
    }
}
