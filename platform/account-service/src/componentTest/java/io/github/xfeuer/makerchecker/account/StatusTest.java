package io.github.xfeuer.makerchecker.account;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.assertj.RestTestClientResponse;

@ComponentTest
class StatusTest {

    @Test
    void reportsItsNameAndTheVersionOfItsBuild(@Autowired RestTestClient client, @Autowired BuildProperties build) {
        RestTestClientResponse response =
                RestTestClientResponse.from(client.get().uri("/v1/status").exchange());

        assertThat(response)
                .hasStatusOk()
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .bodyJson()
                .isStrictlyEqualTo("""
                    {"service": "account-service", "version": "%s"}
                    """.formatted(build.getVersion()));
    }
}
