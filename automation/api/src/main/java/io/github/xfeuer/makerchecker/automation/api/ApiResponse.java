package io.github.xfeuer.makerchecker.automation.api;

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

    /** Only a transport makes a response, so every response that a test sees went through one. */
    ApiResponse(int status, Map<String, String> headers, String text, Class<T> bodyType) {
        this.status = status;
        this.headers = Map.copyOf(headers);
        this.text = text;
        this.bodyType = bodyType;
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
