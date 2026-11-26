package io.github.xfeuer.makerchecker.chassis.problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/** The model of a problem, without Spring around it. */
class ProblemTest {

    @Test
    void namesATypeByTheAddressOfItsPage() {
        assertThat(ProblemType.named("insufficient-funds"))
                .isEqualTo(URI.create("https://xfeuer.github.io/maker-checker/problems/insufficient-funds"));
    }

    @Test
    void givesABlankTypeThePhraseOfItsStatus() {
        ProblemType type = ProblemType.blank(HttpStatus.NOT_FOUND);

        assertThat(type.type()).isEqualTo(URI.create("about:blank"));
        assertThat(type.status()).isEqualTo(404);
        assertThat(type.title()).isEqualTo("Not Found");
    }

    @Test
    void givesAStatusWithoutAPhraseATitleAllTheSame() {
        assertThat(ProblemType.blank(HttpStatusCode.valueOf(499)).title()).isEqualTo("Status 499");
    }

    @Test
    void writesTheMembersOfTheContractInTheOrderOfTheRfc() {
        Problem problem = Problem.of(CommonProblem.INVALID_REQUEST, "A sentence for a person.");

        assertThat(problem.toJson())
                .isEqualTo("{\"type\":\"https://xfeuer.github.io/maker-checker/problems/invalid-request\","
                        + "\"title\":\"The request breaks the contract.\",\"status\":400,"
                        + "\"detail\":\"A sentence for a person.\"}");
    }

    @Test
    void leavesOutTheMembersThatAProblemDoesNotHave() {
        assertThat(Problem.of(ProblemType.blank(HttpStatus.NOT_FOUND)).toJson())
                .isEqualTo("{\"type\":\"about:blank\",\"title\":\"Not Found\",\"status\":404}");
    }

    @Test
    void listsAViolationForEveryStatus400() {
        Problem problem = Problem.ofStatus(400);

        assertThat(problem.type()).isEqualTo(CommonProblem.INVALID_REQUEST.type());
        assertThat(problem.errors()).hasSize(1);
        assertThat(problem.toJson()).contains("\"errors\":[{\"detail\":");
    }

    @Test
    void refusesAnInvalidRequestWithoutAViolation() {
        assertThatThrownBy(() -> new InvalidRequestException(List.of())).isInstanceOf(IllegalArgumentException.class);
    }
}
