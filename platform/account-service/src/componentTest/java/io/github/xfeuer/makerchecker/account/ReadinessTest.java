package io.github.xfeuer.makerchecker.account;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.assertj.RestTestClientResponse;

/** The readiness probe is what the health check of the stack asks. */
@ComponentTest
class ReadinessTest {

    @Test
    void isReadyWhenItsDatabaseAnswers(@Autowired RestTestClient client) {
        RestTestClientResponse response = RestTestClientResponse.from(
                client.get().uri("/actuator/health/readiness").exchange());

        assertThat(response).hasStatusOk().bodyJson().extractingPath("$.status").isEqualTo("UP");
    }
}
