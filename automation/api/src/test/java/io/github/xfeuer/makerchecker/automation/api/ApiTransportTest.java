package io.github.xfeuer.makerchecker.automation.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.APIRequestContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.xfeuer.makerchecker.automation.core.playwright.PlaywrightSession;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Runs the transport through a real Playwright driver, against a server inside this JVM. */
class ApiTransportTest {

    record Thing(String name) {}

    private static HttpServer server;
    private static PlaywrightSession playwright;
    private static String baseUrl;

    private final List<Exchange> exchanges = new ArrayList<>();
    private APIRequestContext request;

    @BeforeAll
    static void startTheServerAndPlaywright() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/v1/thing", http -> respond(http, 200, "application/json", "{\"name\": \"a\"}"));
        server.createContext("/v1/missing", http -> respond(http, 404, "text/plain", "no such thing"));
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        playwright = PlaywrightSession.start();
    }

    @AfterAll
    static void stopTheServerAndPlaywright() {
        playwright.close();
        server.stop(0);
    }

    @BeforeEach
    void openARequestContext() {
        request = playwright.newRequestContext();
    }

    @AfterEach
    void disposeTheRequestContext() {
        request.dispose();
    }

    @Test
    void returnsTheStatusTheHeadersAndTheBody() {
        ApiResponse<Thing> response = transport(baseUrl).get("/v1/thing", Thing.class);

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.headers()).containsEntry("content-type", "application/json");
        assertThat(response.body()).isEqualTo(new Thing("a"));
    }

    @Test
    void tellsItsListenersAboutTheExchange() {
        Instant before = Instant.now();

        transport(baseUrl).get("/v1/thing", Thing.class);

        assertThat(exchanges).singleElement().satisfies(exchange -> {
            assertThat(exchange.request()).isEqualTo(new Exchange.Request("GET", baseUrl + "/v1/thing"));
            assertThat(exchange.response().status()).isEqualTo(200);
            assertThat(exchange.response().statusText()).isEqualTo("OK");
            assertThat(exchange.response().headers()).anySatisfy(header -> {
                assertThat(header.name()).isEqualToIgnoringCase("content-type");
                assertThat(header.value()).isEqualTo("application/json");
            });
            assertThat(exchange.response().body()).isEqualTo("{\"name\": \"a\"}");
            assertThat(exchange.started()).isBetween(before, exchange.finished());
        });
    }

    @Test
    void handsOverAnErrorResponseWithoutReadingItsBodyAsTheType() {
        ApiResponse<Thing> response = transport(baseUrl).get("/v1/missing", Thing.class);

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.text()).isEqualTo("no such thing");
        assertThat(exchanges).singleElement().satisfies(exchange -> {
            assertThat(exchange.response().status()).isEqualTo(404);
            assertThat(exchange.response().statusText()).isEqualTo("Not Found");
            assertThat(exchange.response().body()).isEqualTo("no such thing");
        });
    }

    @Test
    void appendsThePathToABaseUrlThatEndsWithASlash() {
        transport(baseUrl + "/").get("/v1/thing", Thing.class);

        assertThat(exchanges)
                .singleElement()
                .satisfies(exchange -> assertThat(exchange.request().url()).isEqualTo(baseUrl + "/v1/thing"));
    }

    private ApiTransport transport(String url) {
        return new ApiTransport(request, URI.create(url), List.of(exchanges::add));
    }

    private static void respond(HttpExchange http, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        http.getResponseHeaders().add("Content-Type", contentType);
        http.sendResponseHeaders(status, bytes.length);
        try (http) {
            http.getResponseBody().write(bytes);
        }
    }
}
