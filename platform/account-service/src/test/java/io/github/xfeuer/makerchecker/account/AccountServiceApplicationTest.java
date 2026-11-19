package io.github.xfeuer.makerchecker.account;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest
@AutoConfigureMockMvc
class AccountServiceApplicationTest {

    @Test
    void startsAndKnowsTheVersionOfItsBuild(@Autowired BuildProperties build) {
        assertThat(build.getVersion()).isNotBlank();
    }

    @Test
    void exposesTheReadinessProbeForTheHealthCheck(@Autowired MockMvcTester mvc) {
        assertThat(mvc.get().uri("/actuator/health/readiness"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.status")
                .isEqualTo("UP");
    }
}
