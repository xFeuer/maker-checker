package io.github.xfeuer.makerchecker.account;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.xfeuer.makerchecker.chassis.contract.testing.ContractCheck;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.assertj.RestTestClientResponse;

/**
 * The service without its database: it starts, and it says that it is not ready. See ADR 0016.
 *
 * <p>This class is no {@link ComponentTest}: it starts an application of its own, and no container.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(ContractCheck.class)
class ReadinessWithoutDatabaseTest {

    @DynamicPropertySource
    static void aDatabaseThatDoesNotAnswer(DynamicPropertyRegistry settings) throws IOException {
        int port = aPortThatNothingListensOn();
        settings.add("spring.datasource.url", () -> "jdbc:postgresql://127.0.0.1:%d/account".formatted(port));
        settings.add("spring.datasource.username", () -> "account");
        settings.add("spring.datasource.password", () -> "account");
    }

    @Test
    void isNotReadyWhenItsDatabaseDoesNotAnswer(@Autowired RestTestClient client) {
        RestTestClientResponse response = RestTestClientResponse.from(
                client.get().uri("/actuator/health/readiness").exchange());

        assertThat(response)
                .hasStatus(HttpStatus.SERVICE_UNAVAILABLE)
                .bodyJson()
                .extractingPath("$.status")
                .isEqualTo("DOWN");
    }

    @Test
    void stillReportsItsStatus(@Autowired RestTestClient client) {
        RestTestClientResponse response =
                RestTestClientResponse.from(client.get().uri("/v1/status").exchange());

        assertThat(response).hasStatusOk();
    }

    /** The system gave this port out a moment ago and has it back, so a connection to it is refused at once. */
    private static int aPortThatNothingListensOn() throws IOException {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        }
    }
}
