package io.github.xfeuer.makerchecker.automation.api.account;

import java.util.Objects;

/** The body of {@code GET /v1/status}. The contract requires both fields. */
public record ServiceStatus(String service, String version) {

    public ServiceStatus {
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(version, "version");
    }
}
