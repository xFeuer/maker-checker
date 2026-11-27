package io.github.xfeuer.makerchecker.chassis.contract.testing;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.xfeuer.makerchecker.chassis.contract.Contract;
import io.github.xfeuer.makerchecker.chassis.problem.CommonProblem;
import io.github.xfeuer.makerchecker.chassis.problem.ProblemType;
import java.net.URI;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The two comparisons, without an application around them. */
class ContractAssertionsTest {

    private static final Set<String> CONTRACT = Set.of("GET /holds", "GET /holds/{holdId}", "PUT /holds/{holdId}");

    @Test
    void findsNothingWhenTheApplicationMapsItsContract() {
        Set<String> mapped = Set.of("GET /holds", "GET /holds/{id}", "PUT /holds/{id}");

        assertThat(ContractAssertions.operationFindings(CONTRACT, mapped, Set.of()))
                .isEmpty();
    }

    @Test
    void findsAnOperationThatTheApplicationDoesNotMap() {
        Set<String> mapped = Set.of("GET /holds", "GET /holds/{holdId}");

        assertThat(ContractAssertions.operationFindings(CONTRACT, mapped, Set.of()))
                .containsExactly("The contract has PUT /holds/{holdId}, and the application does not map it.");
    }

    @Test
    void waitsForAnOperationOnTheListOfWhatIsNotBuiltYet() {
        Set<String> mapped = Set.of("GET /holds", "GET /holds/{holdId}");

        assertThat(ContractAssertions.operationFindings(CONTRACT, mapped, Set.of("PUT /holds/{holdId}")))
                .isEmpty();
    }

    @Test
    void findsAnOperationThatTheContractDoesNotHave() {
        Set<String> mapped = Set.of("GET /holds", "GET /holds/{holdId}", "PUT /holds/{holdId}", "DELETE /holds");

        assertThat(ContractAssertions.operationFindings(CONTRACT, mapped, Set.of()))
                .containsExactly("The application maps DELETE /holds, and the contract does not have it.");
    }

    @Test
    void findsAnEntryOfTheListThatIsBuiltOrThatTheContractDoesNotHave() {
        Set<String> waiting = Set.of("GET /holds", "GET /cups");

        assertThat(ContractAssertions.operationFindings(CONTRACT, CONTRACT, waiting))
                .containsExactlyInAnyOrder(
                        "GET /holds is built. It has to leave the list of what is not built yet.",
                        "The list of what is not built yet has GET /cups, and the contract does not.");
    }

    @Test
    void findsNothingWhenTheTypesOfProblemAreTheOnesOfTheContract() {
        Set<Contract.ProblemExample> contract = Set.of(
                example("invalid-request", 400, "The request breaks the contract."),
                example("teapot-empty", 422, "The teapot is empty."));

        assertThat(ContractAssertions.problemFindings(
                        contract, List.of(type("teapot-empty", 422, "The teapot is empty.")), shared(), Set.of()))
                .isEmpty();
    }

    @Test
    void findsATypeWithAnotherStatusOrAnotherTitleThanTheContractHas() {
        Set<Contract.ProblemExample> contract = Set.of(example("teapot-empty", 422, "The teapot is empty."));

        assertThat(ContractAssertions.problemFindings(
                        contract, List.of(type("teapot-empty", 409, "No tea.")), List.of(), Set.of()))
                .containsExactly(
                        "teapot-empty is 409 \"No tea.\" in the code, and 422 \"The teapot is empty.\" in the contract.");
    }

    @Test
    void findsATypeThatOnlyOneSideHas() {
        Set<Contract.ProblemExample> contract = Set.of(example("teapot-empty", 422, "The teapot is empty."));

        assertThat(ContractAssertions.problemFindings(
                        contract, List.of(type("cup-broken", 409, "The cup is broken.")), List.of(), Set.of()))
                .containsExactlyInAnyOrder(
                        "The application declares cup-broken, and the contract does not name it.",
                        "The contract names teapot-empty, and the application does not declare it.");
    }

    @Test
    void waitsForATypeOnTheListAndLetsATypeOfTheChassisBeThatTheContractDoesNotName() {
        Set<Contract.ProblemExample> contract = Set.of(example("teapot-empty", 422, "The teapot is empty."));

        assertThat(ContractAssertions.problemFindings(contract, List.of(), shared(), Set.of("teapot-empty")))
                .isEmpty();
    }

    private static List<ProblemType> shared() {
        return List.of(CommonProblem.values());
    }

    private static Contract.ProblemExample example(String name, int status, String title) {
        return new Contract.ProblemExample(ProblemType.named(name), status, title);
    }

    private static ProblemType type(String name, int status, String title) {
        URI address = ProblemType.named(name);
        return new ProblemType() {
            @Override
            public URI type() {
                return address;
            }

            @Override
            public int status() {
                return status;
            }

            @Override
            public String title() {
                return title;
            }
        };
    }
}
