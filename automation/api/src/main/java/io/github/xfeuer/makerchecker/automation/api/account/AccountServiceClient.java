package io.github.xfeuer.makerchecker.automation.api.account;

import io.github.xfeuer.makerchecker.automation.api.ApiResponse;
import io.github.xfeuer.makerchecker.automation.api.ApiTransport;
import java.util.Objects;

/** The typed client of account-service. Each method is one operation of its contract. */
public final class AccountServiceClient {

    private final ApiTransport transport;

    public AccountServiceClient(ApiTransport transport) {
        this.transport = Objects.requireNonNull(transport, "transport");
    }

    /** {@code GET /v1/status}. */
    public ApiResponse<ServiceStatus> status() {
        return transport.get("/v1/status", ServiceStatus.class);
    }
}
