package io.github.xfeuer.makerchecker.automation.allure;

import io.github.xfeuer.makerchecker.automation.api.Exchange;
import io.github.xfeuer.makerchecker.automation.api.ExchangeListener;
import io.qameta.allure.Allure;
import io.qameta.allure.http.HttpExchange;
import io.qameta.allure.http.HttpExchangeBody;
import io.qameta.allure.http.HttpExchangeCookie;
import io.qameta.allure.http.HttpExchangeResponse;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Attaches every exchange to the Allure result of the test that made it.
 *
 * <p>No credential reaches the report through a header. Allure replaces the value of a header that carries one, such
 * as {@code Authorization}. It turns a {@code Set-Cookie} header into a cookie and would show its value, so this
 * listener has it replace the value of every cookie too. The name and the attributes of a cookie stay readable.
 */
public final class AllureExchangeListener implements ExchangeListener {

    @Override
    public void onExchange(Exchange exchange) {
        Allure.addHttpExchange(name(exchange.request()), httpExchange(exchange));
    }

    /** The method and the path, such as {@code GET /v1/status}. The attachment itself holds the whole URL. */
    private static String name(Exchange.Request request) {
        URI url = URI.create(request.url());
        String query = url.getRawQuery() == null ? "" : "?" + url.getRawQuery();
        return request.method() + " " + url.getRawPath() + query;
    }

    private static HttpExchange httpExchange(Exchange exchange) {
        HttpExchangeResponse response = response(exchange.response());
        return HttpExchange.builder()
                .request(exchange.request().method(), exchange.request().url())
                .response(response)
                .redactCookies(cookieNames(response))
                .start(exchange.started().toEpochMilli())
                .stop(exchange.finished().toEpochMilli())
                .build();
    }

    private static List<String> cookieNames(HttpExchangeResponse response) {
        // Allure keeps no list at all for a response without cookies.
        List<HttpExchangeCookie> cookies = response.cookies() == null ? List.of() : response.cookies();
        return cookies.stream().map(HttpExchangeCookie::name).toList();
    }

    private static HttpExchangeResponse response(Exchange.Response response) {
        HttpExchangeResponse.Builder builder =
                HttpExchangeResponse.builder().setStatus(response.status()).setStatusText(response.statusText());
        response.headers().forEach(header -> builder.addHeader(header.name(), header.value()));
        return builder.setBody(body(response)).build();
    }

    private static HttpExchangeBody body(Exchange.Response response) {
        String contentType = response.headers().stream()
                .filter(header -> header.name().equalsIgnoreCase("Content-Type"))
                .map(Exchange.Header::value)
                .findFirst()
                .orElse(null);
        long size = response.body().getBytes(StandardCharsets.UTF_8).length;
        return new HttpExchangeBody(contentType, "utf8", response.body(), size, null, null, null, null);
    }
}
