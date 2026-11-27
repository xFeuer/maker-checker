package io.github.xfeuer.makerchecker.account;

import io.github.xfeuer.makerchecker.chassis.contract.Contract;
import io.github.xfeuer.makerchecker.chassis.contract.testing.ContractAssertions;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * The service and its contract as a whole: the operations and the types of problem. See ADR 0019.
 *
 * <p>The client of every component test already holds each response to the contract. These two tests hold what no
 * single response shows.
 *
 * <p>The two lists name what the contract has and the service does not build yet. The change that builds an entry
 * takes it off its list, and the test fails for an entry that is built and still listed.
 */
@ComponentTest
class ContractTest {

    private static final Set<String> OPERATIONS_NOT_BUILT_YET = Set.of(
            "GET /v1/accounts",
            "GET /v1/accounts/{accountId}",
            "GET /v1/holds",
            "GET /v1/holds/{holdId}",
            "PUT /v1/holds/{holdId}",
            "PUT /v1/holds/{holdId}/release",
            "GET /v1/ledger-transactions",
            "GET /v1/ledger-transactions/{ledgerTransactionId}",
            "PUT /v1/ledger-transactions/{ledgerTransactionId}");

    private static final Set<String> PROBLEM_TYPES_NOT_BUILT_YET = Set.of(
            "hold-exists",
            "hold-not-open",
            "insufficient-funds",
            "insufficient-scope",
            "ledger-transaction-exists",
            "unbalanced-ledger-transaction",
            "unknown-account",
            "unknown-hold");

    /** This is the one test that looks inside the service: it reads the mappings of Spring MVC. */
    @Test
    void mapsEveryOperationOfItsContractAndNoOther(
            @Autowired Contract contract,
            @Autowired @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mappings) {
        ContractAssertions.assertOperations(
                contract, mappings, AccountServiceApplication.class.getPackageName(), OPERATIONS_NOT_BUILT_YET);
    }

    /** The service declares no type of its own yet. The chassis has the one that every application answers with. */
    @Test
    void declaresEveryTypeOfProblemAsItsContractHasIt(@Autowired Contract contract) {
        ContractAssertions.assertProblemTypes(contract, List.of(), PROBLEM_TYPES_NOT_BUILT_YET);
    }
}
