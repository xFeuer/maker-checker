package io.github.xfeuer.makerchecker.automation.core.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;

/** Where the system under test runs. The defaults match the local Compose stack. */
public record Configuration(URI accountServiceUrl) {

    public Configuration {
        Objects.requireNonNull(accountServiceUrl, "accountServiceUrl");
    }

    /** Reads the configuration from the environment variables of this process. */
    public static Configuration load() {
        return from(System.getenv());
    }

    static Configuration from(Map<String, String> environment) {
        return new Configuration(url(environment, "ACCOUNT_SERVICE_URL", "http://localhost:8080"));
    }

    private static URI url(Map<String, String> environment, String variable, String fallback) {
        String value = environment.getOrDefault(variable, fallback);
        String message = "%s must be an absolute URL, but was: %s".formatted(variable, value);
        URI url;
        try {
            url = new URI(value);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(message, e);
        }
        if (url.getScheme() == null || url.getHost() == null) {
            throw new IllegalArgumentException(message);
        }
        return url;
    }
}
