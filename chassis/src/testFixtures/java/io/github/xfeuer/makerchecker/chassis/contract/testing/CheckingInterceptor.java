package io.github.xfeuer.makerchecker.chassis.contract.testing;

import io.github.xfeuer.makerchecker.chassis.contract.Contract;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/** Checks each response on its way back to the test, and fails the test when it breaks the contract. */
final class CheckingInterceptor implements ClientHttpRequestInterceptor {

    private final Contract contract;

    CheckingInterceptor(Contract contract) {
        this.contract = contract;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        ClientHttpResponse response = execution.execute(request, body);
        Optional<Contract.Operation> operation = contract.operationFor(
                request.getMethod().name(), request.getURI().getPath());
        if (operation.isEmpty()) {
            // The contract says nothing about this request, such as one for a probe of the actuator.
            return response;
        }
        byte[] answer = response.getBody().readAllBytes();
        List<String> violations = operation
                .get()
                .violationsOf(
                        response.getStatusCode().value(),
                        response.getHeaders(),
                        new String(answer, StandardCharsets.UTF_8));
        if (!violations.isEmpty()) {
            throw new AssertionError("The response of %s with the status %d breaks the contract:\n  %s"
                    .formatted(
                            operation.get().name(), response.getStatusCode().value(), String.join("\n  ", violations)));
        }
        return new ReadResponse(response, answer);
    }

    /** The response again, for the test that still wants to read the body. */
    private record ReadResponse(ClientHttpResponse response, byte[] answer) implements ClientHttpResponse {

        @Override
        public HttpStatusCode getStatusCode() throws IOException {
            return response.getStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return response.getStatusText();
        }

        @Override
        public HttpHeaders getHeaders() {
            return response.getHeaders();
        }

        @Override
        public InputStream getBody() {
            return new ByteArrayInputStream(answer);
        }

        @Override
        public void close() {
            response.close();
        }
    }
}
