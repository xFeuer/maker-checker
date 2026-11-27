package io.github.xfeuer.makerchecker.chassis.contract.testing;

import io.github.xfeuer.makerchecker.chassis.contract.Contract;
import org.springframework.boot.resttestclient.autoconfigure.RestTestClientBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Holds every response that a test receives to the specification of the application. See ADR 0019.
 *
 * <p>A test class imports this. The {@code RestTestClient} that it then gets checks each response by itself, on the
 * side of the client: its status, its media type, its headers and its body. A response that breaks the contract
 * fails the test at the call that received it. A path that the contract does not have is not checked.
 */
@TestConfiguration(proxyBeanMethods = false)
public class ContractCheck {

    /** Where the build puts the bundle of the API of an application. */
    public static final String SPECIFICATION = "contracts/openapi.json";

    @Bean
    Contract contract() {
        return Contract.ofResource(SPECIFICATION);
    }

    @Bean
    RestTestClientBuilderCustomizer contractCheck(Contract contract) {
        return client -> client.requestInterceptor(new CheckingInterceptor(contract));
    }
}
