package io.github.xfeuer.makerchecker.account.status;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(StatusController.class)
class StatusControllerTest {

    @Test
    void reportsTheServiceNameAndTheBuildVersion(@Autowired MockMvcTester mvc) {
        assertThat(mvc.get().uri("/v1/status"))
                .hasStatusOk()
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .bodyJson()
                .isStrictlyEqualTo("""
                    {"service": "account-service", "version": "1.2.3"}
                    """);
    }

    @TestConfiguration
    static class FixedBuildProperties {

        @Bean
        BuildProperties buildProperties() {
            Properties entries = new Properties();
            entries.setProperty("version", "1.2.3");
            return new BuildProperties(entries);
        }
    }
}
