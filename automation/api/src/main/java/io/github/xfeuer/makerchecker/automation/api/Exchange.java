package io.github.xfeuer.makerchecker.automation.api;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * One request and the response to it, as the framework sent and received them.
 *
 * <p>The request is what a client asked Playwright to send. Headers that Playwright adds on its own are not in it.
 *
 * @param started when the framework handed the request to Playwright
 * @param finished when Playwright had the response
 */
public record Exchange(Request request, Response response, Instant started, Instant finished) {

    public Exchange {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(response, "response");
        Objects.requireNonNull(started, "started");
        Objects.requireNonNull(finished, "finished");
    }

    /** What a client sent. */
    public record Request(String method, String url) {

        public Request {
            Objects.requireNonNull(method, "method");
            Objects.requireNonNull(url, "url");
        }
    }

    /** What the service answered. The headers keep their order, and a name can come more than once. */
    public record Response(int status, String statusText, List<Header> headers, String body) {

        public Response {
            Objects.requireNonNull(statusText, "statusText");
            headers = List.copyOf(headers);
            Objects.requireNonNull(body, "body");
        }
    }

    /** One header of a response. */
    public record Header(String name, String value) {

        public Header {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(value, "value");
        }
    }
}
