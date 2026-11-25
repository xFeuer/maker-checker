package io.github.xfeuer.makerchecker.buildlogic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RulesetTestResultTest {

    @Test
    void findsNothingWhenEveryCasePassed() {
        String result = """
                {"version": 1, "status": "passed", "exitCode": 0, "errors": [], "cases": [
                  {"id": "keeps-the-rules", "status": "passed", "failures": [], "errors": []},
                  {"id": "breaks-no-patch", "status": "passed", "failures": [], "errors": []}
                ]}
                """;

        assertThat(RulesetTestResult.problems(result)).isEmpty();
    }

    @Test
    void namesTheRuleThatDidNotFire() {
        String result = """
                {"version": 1, "status": "failed", "exitCode": 1, "errors": [], "cases": [
                  {"id": "breaks-no-patch", "status": "failed", "errors": [], "failures": [
                    {"assertion": "expect.diagnostics[0]", "code": "missing_diagnostic",
                     "expected": {"rule": "no-patch"}, "actual": null}
                  ]}
                ]}
                """;

        assertThat(RulesetTestResult.problems(result))
                .containsExactly("breaks-no-patch: vacuum did not report no-patch.");
    }

    @Test
    void namesAFindingThatACaseDoesNotExpect() {
        String result = """
                {"version": 1, "status": "failed", "exitCode": 1, "errors": [], "cases": [
                  {"id": "keeps-the-rules", "status": "failed", "errors": [], "failures": [
                    {"assertion": "expect.diagnostics", "code": "unexpected_diagnostic", "expected": null,
                     "actual": {"rule": "no-number-type", "severity": "error", "line": 31, "column": 15,
                                "source": "keeps-the-rules.yaml", "message": "No schema has the type number."}}
                  ]}
                ]}
                """;

        assertThat(RulesetTestResult.problems(result))
                .containsExactly("keeps-the-rules: vacuum reported no-number-type at keeps-the-rules.yaml:31,"
                        + " which the case does not expect: No schema has the type number.");
    }

    @Test
    void namesWhatKeptACaseFromRunning() {
        String result = """
                {"version": 1, "status": "error", "exitCode": 2, "errors": [], "cases": [
                  {"id": "breaks-no-patch", "status": "error", "failures": [], "errors": [
                    {"code": "input_error", "phase": "before", "caseId": "breaks-no-patch",
                     "message": "open breaks-no-patch.yaml: no such file or directory"}
                  ]}
                ]}
                """;

        assertThat(RulesetTestResult.problems(result))
                .containsExactly("breaks-no-patch: open breaks-no-patch.yaml: no such file or directory");
    }
}
