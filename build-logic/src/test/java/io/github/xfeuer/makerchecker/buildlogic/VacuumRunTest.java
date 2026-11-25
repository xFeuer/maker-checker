package io.github.xfeuer.makerchecker.buildlogic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VacuumRunTest {

    private static final String NO_SERVERS =
            "account-service/openapi.yaml:1:1 warn oas3-api-servers \"no servers defined for the specification\"";
    private static final String NO_DESCRIPTION =
            "account-service/openapi.yaml:3:1 error info-description \"`info` section must have a `description`\"";

    @Test
    void isCleanWhenVacuumReportsNothing() {
        VacuumRun run = new VacuumRun(0, "", "");

        assertThat(run.findings()).isEmpty();
        assertThat(run.failure()).isEmpty();
    }

    @Test
    void listsEveryFinding() {
        VacuumRun run = new VacuumRun(
                1,
                NO_SERVERS + "\n" + NO_DESCRIPTION + "\n",
                "failed with 1 errors, 1 warnings, 0 informs and 0 hints\n");

        assertThat(run.findings()).containsExactly(NO_SERVERS, NO_DESCRIPTION);
        assertThat(run.failure()).contains("""
                The specifications do not lint clean. vacuum has 2 findings:
                  %s
                  %s""".formatted(NO_SERVERS, NO_DESCRIPTION));
    }

    @Test
    void countsASingleFinding() {
        VacuumRun run = new VacuumRun(1, NO_SERVERS + "\r\n", "");

        assertThat(run.failure()).contains("""
                The specifications do not lint clean. vacuum has 1 finding:
                  %s""".formatted(NO_SERVERS));
    }

    @Test
    void isNotCleanWhenVacuumReportsAFindingAndStillEndsWithZero() {
        VacuumRun run = new VacuumRun(0, NO_SERVERS + "\n", "");

        assertThat(run.failure()).isPresent();
    }

    @Test
    void showsWhatVacuumWroteWhenItCouldNotLint() {
        VacuumRun run = new VacuumRun(2, "", "failed to read file 'account-service/openapi.yaml'\n");

        assertThat(run.failure()).contains("""
                vacuum could not lint the specifications. It ended with exit code 2 and wrote:
                  failed to read file 'account-service/openapi.yaml'""");
    }

    @Test
    void takesAnExitCodeWithoutAFindingAsAFailureOfVacuum() {
        VacuumRun run = new VacuumRun(1, "", "");

        assertThat(run.failure())
                .contains("vacuum could not lint the specifications. It ended with exit code 1 and wrote:");
    }
}
