package io.github.xfeuer.makerchecker.account.status;

import java.util.Objects;

/** The body of {@code GET /v1/status}. The contract requires both fields. */
record ServiceStatus(String service, String version) {

    ServiceStatus {
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(version, "version");
    }
}
