package io.github.xfeuer.makerchecker.automation.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConfigurationTest {

    @Test
    void pointsAtTheLocalStackByDefault() {
        Configuration configuration = Configuration.from(Map.of());

        assertThat(configuration.accountServiceUrl()).isEqualTo(URI.create("http://localhost:8080"));
    }

    @Test
    void takesTheAccountServiceUrlFromTheEnvironment() {
        Configuration configuration = Configuration.from(Map.of("ACCOUNT_SERVICE_URL", "https://accounts.test:8443"));

        assertThat(configuration.accountServiceUrl()).isEqualTo(URI.create("https://accounts.test:8443"));
    }

    @Test
    void rejectsAValueThatIsNotAnAbsoluteUrl() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Configuration.from(Map.of("ACCOUNT_SERVICE_URL", "localhost:8080")))
                .withMessage("ACCOUNT_SERVICE_URL must be an absolute URL, but was: localhost:8080");
    }
}
