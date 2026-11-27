package io.github.xfeuer.makerchecker.chassis.testapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.xfeuer.makerchecker.chassis.contract.testing.ContractCheck;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.assertj.RestTestClientResponse;

/**
 * The check of every response, as a test of an application gets it: the test imports it, and its client does the
 * rest. The contract of the test application is {@code contracts/openapi.json} in the resources of these tests.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(ContractCheck.class)
class ContractCheckTest {

    @Autowired
    RestTestClient client;

    @Test
    void letsAResponsePassThatKeepsTheContractAndLeavesItsBodyToTheTest() {
        RestTestClientResponse response =
                RestTestClientResponse.from(client.get().uri("/works").exchange());

        assertThat(response).hasStatusOk().bodyJson().isStrictlyEqualTo("""
            {"answer": "yes"}
            """);
    }

    @Test
    void failsTheTestAtTheCallWhoseResponseBreaksTheContract() {
        assertThatThrownBy(() -> client.get().uri("/breaks-the-contract").exchange())
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining(
                        "The response of GET /breaks-the-contract with the status 200 breaks the contract")
                .hasMessageContaining("The header X-Kettle is required")
                .hasMessageContaining("kettle");
    }

    @Test
    void holdsAnErrorResponseToTheContractToo() {
        // An order without a body is a 400, and the contract of the test application has no response for it.
        assertThatThrownBy(() -> client.post().uri("/orders").exchange())
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("POST /orders")
                .hasMessageContaining("The contract has no response with the status 400");
    }

    @Test
    void leavesARequestAloneThatTheContractSaysNothingAbout() {
        RestTestClientResponse response =
                RestTestClientResponse.from(client.get().uri("/nowhere").exchange());

        assertThat(response).hasStatus(404);
    }
}
