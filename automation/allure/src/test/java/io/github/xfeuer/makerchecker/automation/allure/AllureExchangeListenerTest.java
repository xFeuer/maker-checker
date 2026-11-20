package io.github.xfeuer.makerchecker.automation.allure;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.xfeuer.makerchecker.automation.api.Exchange;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureExternalKey;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.AllureResultsWriter;
import io.qameta.allure.http.HttpExchange;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class AllureExchangeListenerTest {

    private static final Instant STARTED = Instant.parse("2026-11-20T06:10:11.100Z");
    private static final Instant FINISHED = Instant.parse("2026-11-20T06:10:11.151Z");

    private final Results results = new Results();
    private final AllureExchangeListener listener = new AllureExchangeListener();
    private AllureLifecycle previous;

    @BeforeEach
    void writeTheResultsToMemory() {
        previous = Allure.getLifecycle();
        Allure.setLifecycle(new AllureLifecycle(results));
    }

    @AfterEach
    void restoreTheLifecycle() {
        Allure.setLifecycle(previous);
    }

    @Test
    void attachesTheExchangeToTheRunningTest() {
        Exchange exchange = exchange(
                "http://localhost:8080/v1/status",
                List.of(new Exchange.Header("Content-Type", "application/json")),
                "{\"service\":\"account-service\"}");

        inATest(() -> listener.onExchange(exchange));

        Attachment attachment = theAttachment();
        assertThat(attachment.getType()).isEqualTo(HttpExchange.CONTENT_TYPE);
        JsonNode json = results.json(attachment);
        assertThat(json.at("/request/method").asString()).isEqualTo("GET");
        assertThat(json.at("/request/url").asString()).isEqualTo("http://localhost:8080/v1/status");
        assertThat(json.at("/response/status").asInt()).isEqualTo(200);
        assertThat(json.at("/response/statusText").asString()).isEqualTo("OK");
        assertThat(json.at("/response/headers/0/name").asString()).isEqualTo("Content-Type");
        assertThat(json.at("/response/headers/0/value").asString()).isEqualTo("application/json");
        assertThat(json.at("/response/body/contentType").asString()).isEqualTo("application/json");
        assertThat(json.at("/response/body/value").asString()).isEqualTo("{\"service\":\"account-service\"}");
        assertThat(json.at("/start").asLong()).isEqualTo(STARTED.toEpochMilli());
        assertThat(json.at("/stop").asLong()).isEqualTo(FINISHED.toEpochMilli());
    }

    @Test
    void namesTheAttachmentAfterTheMethodAndThePath() {
        inATest(() -> listener.onExchange(exchange("http://localhost:8080/v1/accounts?owner=42", List.of(), "[]")));

        assertThat(theAttachment().getName()).isEqualTo("GET /v1/accounts?owner=42");
    }

    @Test
    void hidesTheValueOfACookieThatTheServiceSets() {
        Exchange exchange = exchange(
                "http://localhost:8080/v1/status",
                List.of(new Exchange.Header("Set-Cookie", "session=s3cr3t; Path=/; HttpOnly")),
                "{}");

        inATest(() -> listener.onExchange(exchange));

        Attachment attachment = theAttachment();
        assertThat(results.text(attachment)).doesNotContain("s3cr3t");
        JsonNode cookie = results.json(attachment).at("/response/cookies/0");
        assertThat(cookie.at("/name").asString()).isEqualTo("session");
        assertThat(cookie.at("/value").asString()).isEqualTo(HttpExchange.REDACTED_VALUE);
        assertThat(cookie.at("/httpOnly").asBoolean()).isTrue();
    }

    private static Exchange exchange(String url, List<Exchange.Header> headers, String body) {
        return new Exchange(
                new Exchange.Request("GET", url), new Exchange.Response(200, "OK", headers, body), STARTED, FINISHED);
    }

    /** Runs an action the way a test adapter would: between the start and the end of an Allure test. */
    private static void inATest(Runnable action) {
        AllureLifecycle lifecycle = Allure.getLifecycle();
        AllureExternalKey key = AllureExternalKey.random(AllureExchangeListenerTest.class);
        lifecycle.scheduleTest(key, new TestResult().setName("a test"));
        lifecycle.startTest(key);
        try {
            action.run();
        } finally {
            lifecycle.stopTest(key);
            lifecycle.writeTest(key);
        }
    }

    /** Allure shows an exchange as a step that holds one attachment. */
    private Attachment theAttachment() {
        assertThat(results.tests).singleElement();
        List<StepResult> steps = results.tests.getFirst().getSteps();
        assertThat(steps).singleElement();
        assertThat(steps.getFirst().getAttachments()).singleElement();
        return steps.getFirst().getAttachments().getFirst();
    }

    /** Keeps what Allure would write into the results directory. */
    private static final class Results implements AllureResultsWriter {

        private final List<TestResult> tests = new ArrayList<>();
        private final Map<String, String> attachments = new HashMap<>();

        @Override
        public void write(TestResult testResult) {
            tests.add(testResult);
        }

        @Override
        public void write(TestResultContainer testResultContainer) {
            // The listener writes no containers.
        }

        @Override
        public void write(String source, InputStream attachment) {
            try {
                attachments.put(source, new String(attachment.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        String text(Attachment attachment) {
            return attachments.get(attachment.getSource());
        }

        JsonNode json(Attachment attachment) {
            return JsonMapper.shared().readTree(text(attachment));
        }
    }
}
