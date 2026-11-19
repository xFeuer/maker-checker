package io.github.xfeuer.makerchecker.automation.api.account;

import com.microsoft.playwright.APIRequestContext;
import io.github.xfeuer.makerchecker.automation.api.ApiResponse;
import java.net.URI;
import java.util.Objects;

/** The typed client of account-service. Each method is one operation of its contract. */
public final class AccountServiceClient {

    private final APIRequestContext request;
    private final String baseUrl;

    public AccountServiceClient(APIRequestContext request, URI baseUrl) {
        this.request = Objects.requireNonNull(request, "request");
        // Kept without a trailing slash, so that a path is appended exactly as the contract writes it.
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl").toString().replaceFirst("/+$", "");
    }

    /** {@code GET /v1/status}. */
    public ApiResponse<ServiceStatus> status() {
        return ApiResponse.from(request.get(baseUrl + "/v1/status"), ServiceStatus.class);
    }
}
