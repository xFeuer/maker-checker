package io.github.xfeuer.makerchecker.automation.api;

import com.microsoft.playwright.APIResponse;
import java.util.Map;

/**
 * What a service answered: the status, the headers and the body.
 *
 * <p>The body is read on demand. A test can check the status of an error response first, without tripping over a
 * body that does not fit the expected type.
 *
 * @param <T> the type that the contract declares for the body
 */
public final class ApiResponse<T> {

    private final int status;
    private final Map<String, String> headers;
    private final String text;
    private final Class<T> bodyType;

    private ApiResponse(int status, Map<String, String> headers, String text, Class<T> bodyType) {
        this.status = status;
        this.headers = headers;
        this.text = text;
        this.bodyType = bodyType;
    }

    /** Copies what a Playwright response holds, then releases that response. */
    public static <T> ApiResponse<T> from(APIResponse response, Class<T> bodyType) {
        try {
            return new ApiResponse<>(response.status(), Map.copyOf(response.headers()), response.text(), bodyType);
        } finally {
            response.dispose();
        }
    }

    public int status() {
        return status;
    }

    public Map<String, String> headers() {
        return headers;
    }

    /** The body as the service sent it. */
    public String text() {
        return text;
    }

    /** The body as the contract declares it. It fails on a field that the type does not have. */
    public T body() {
        return Json.read(text, bodyType);
    }
}
