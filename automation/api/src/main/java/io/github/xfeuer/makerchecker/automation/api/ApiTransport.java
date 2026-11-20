package io.github.xfeuer.makerchecker.automation.api;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Sends the requests of one service through Playwright.
 *
 * <p>Every typed client goes through a transport. That makes it the one place that sees each request together with
 * its response, and it tells its listeners about every such exchange.
 */
public final class ApiTransport {

    private final APIRequestContext request;
    private final String baseUrl;
    private final List<ExchangeListener> listeners;

    public ApiTransport(APIRequestContext request, URI baseUrl, List<ExchangeListener> listeners) {
        this.request = Objects.requireNonNull(request, "request");
        // Kept without a trailing slash, so that a path is appended exactly as the contract writes it.
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl").toString().replaceFirst("/+$", "");
        this.listeners = List.copyOf(listeners);
    }

    /** Sends a {@code GET} to a path of the service. The path starts with a slash, as in the contract. */
    public <T> ApiResponse<T> get(String path, Class<T> bodyType) {
        String url = baseUrl + path;
        Instant started = Instant.now();
        APIResponse response = request.get(url);
        try {
            Instant finished = Instant.now();
            Exchange exchange = new Exchange(new Exchange.Request("GET", url), read(response), started, finished);
            listeners.forEach(listener -> listener.onExchange(exchange));
            return new ApiResponse<>(
                    response.status(), response.headers(), exchange.response().body(), bodyType);
        } finally {
            response.dispose();
        }
    }

    private static Exchange.Response read(APIResponse response) {
        List<Exchange.Header> headers = response.headersArray().stream()
                .map(header -> new Exchange.Header(header.name, header.value))
                .toList();
        return new Exchange.Response(response.status(), response.statusText(), headers, response.text());
    }
}
