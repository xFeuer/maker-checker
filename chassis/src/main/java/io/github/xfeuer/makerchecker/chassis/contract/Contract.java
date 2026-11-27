package io.github.xfeuer.makerchecker.chassis.contract;

import com.networknt.schema.Error;
import com.networknt.schema.ExecutionConfig;
import com.networknt.schema.InputFormat;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.dialect.Dialects;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The specification of an application: the bundle that the build puts into its jar. See ADR 0019.
 *
 * <p>It says what the contract allows: which operations there are, what a response of an operation may be, and which
 * types of problem the examples show. A JSON Schema validator checks a body and the value of a header, and the rest
 * of OpenAPI is read here: the path of an operation, the status of a response, its media type and its headers.
 */
public final class Contract {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Set<String> METHODS = Set.of("get", "put", "post", "delete", "patch", "head", "options");
    private static final String SCHEMAS = "#/components/schemas/";

    /** A format of the contract is a rule, such as uuid or date-time. JSON Schema alone takes it as a note. */
    private static final ExecutionConfig FORMATS_ARE_RULES =
            new ExecutionConfig.Builder().formatAssertionsEnabled(true).build();

    /** The name under which the validator knows the document. A schema is a pointer into it. */
    private final String location;

    private final JsonNode document;
    private final SchemaRegistry schemas;
    private final List<Operation> operations = new ArrayList<>();

    /** Reads a bundle from the classpath, such as {@code contracts/openapi.json}. */
    public static Contract ofResource(String resource) {
        try (InputStream bundle = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (bundle == null) {
                throw new IllegalStateException(("The classpath has no specification at %s. The build puts the bundle"
                                + " of an application there: see docs/adr/0019-a-service-held-to-its-contract.md.")
                        .formatted(resource));
            }
            return new Contract("classpath:" + resource, new String(bundle.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    Contract(String location, String json) {
        this.location = location;
        this.document = JSON.readTree(json);
        this.schemas =
                SchemaRegistry.withDialect(Dialects.getOpenApi31(), builder -> builder.schemas(Map.of(location, json)));
        refuseAReferenceOutsideTheSchemas(document, "");
        document.path("paths")
                .properties()
                .forEach(path -> path.getValue().properties().stream()
                        .filter(member -> METHODS.contains(member.getKey()))
                        .forEach(member ->
                                operations.add(new Operation(member.getKey(), path.getKey(), member.getValue()))));
    }

    /**
     * A bundle has references to its schemas alone, and the validator follows those. This class follows none: a
     * response or a header behind a reference would go unchecked, so such a document is refused as a whole.
     */
    private void refuseAReferenceOutsideTheSchemas(JsonNode node, String pointer) {
        JsonNode reference = node.path("$ref");
        if (reference.isString() && !reference.stringValue().startsWith(SCHEMAS)) {
            throw new IllegalStateException(("The specification %s has a reference at %s that is none to a schema of"
                            + " its own: %s. The contract reads a bundle, which has no such reference.")
                    .formatted(location, pointer.isEmpty() ? "its root" : pointer, reference.stringValue()));
        }
        node.properties()
                .forEach(member ->
                        refuseAReferenceOutsideTheSchemas(member.getValue(), pointer + "/" + segment(member.getKey())));
        for (int index = 0; node.isArray() && index < node.size(); index++) {
            refuseAReferenceOutsideTheSchemas(node.get(index), pointer + "/" + index);
        }
    }

    /** Every operation, by its name: the method and the path, such as {@code GET /v1/accounts/{accountId}}. */
    public Set<String> operations() {
        Set<String> names = new LinkedHashSet<>();
        operations.forEach(operation -> names.add(operation.name()));
        return names;
    }

    /**
     * The operation that answers a request, when the contract has one. A path without a parameter comes before a
     * path that has one in the same place.
     */
    public Optional<Operation> operationFor(String method, String path) {
        return operations.stream()
                .filter(operation -> operation.method.equalsIgnoreCase(method))
                .filter(operation -> operation.pattern.matcher(path).matches())
                .min(Comparator.comparingInt(operation -> operation.parameters));
    }

    /** Every type of problem that an example of the contract shows, but for {@code about:blank}. */
    public Set<ProblemExample> problemTypes() {
        Set<ProblemExample> examples = new LinkedHashSet<>();
        collectProblems(document, examples);
        return examples;
    }

    private static void collectProblems(JsonNode node, Set<ProblemExample> examples) {
        if (node.isObject()
                && node.path("type").isString()
                && node.path("type").stringValue().startsWith("http")
                && node.path("status").isInt()
                && node.path("title").isString()) {
            examples.add(new ProblemExample(
                    URI.create(node.path("type").stringValue()),
                    node.path("status").intValue(),
                    node.path("title").stringValue()));
        }
        node.forEach(child -> collectProblems(child, examples));
    }

    /** A type of problem as an example of the contract shows it. */
    public record ProblemExample(URI type, int status, String title) {}

    /** One operation of the contract. */
    public final class Operation {

        private final String method;
        private final String path;
        private final JsonNode definition;
        private final Pattern pattern;
        private final int parameters;

        private Operation(String method, String path, JsonNode definition) {
            this.method = method;
            this.path = path;
            this.definition = definition;
            StringBuilder regex = new StringBuilder();
            int count = 0;
            for (String segment : path.split("/", -1)) {
                boolean parameter = segment.startsWith("{") && segment.endsWith("}");
                regex.append(regex.isEmpty() ? "" : "/").append(parameter ? "[^/]+" : Pattern.quote(segment));
                count += parameter ? 1 : 0;
            }
            this.pattern = Pattern.compile(regex.toString());
            this.parameters = count;
        }

        /** The method and the path, such as {@code PUT /v1/holds/{holdId}}. */
        public String name() {
            return method.toUpperCase(Locale.ROOT) + " " + path;
        }

        /**
         * What a response breaks of the contract: nothing, when the list is empty. The contract names the responses
         * of an operation by their status, and {@code default} stands for every other status.
         */
        public List<String> violationsOf(int status, HttpHeaders headers, String body) {
            String key = definition.path("responses").has(String.valueOf(status)) ? String.valueOf(status) : "default";
            JsonNode response = definition.path("responses").path(key);
            if (response.isMissingNode()) {
                return List.of("The contract has no response with the status %d.".formatted(status));
            }
            List<String> violations = new ArrayList<>();
            response.path("headers").properties().forEach(header -> {
                String name = header.getKey();
                String value = headers.getFirst(name);
                if (value == null) {
                    if (header.getValue().path("required").asBoolean(false)) {
                        violations.add("The header %s is required, and the response has none.".formatted(name));
                    }
                } else if (header.getValue().has("schema")) {
                    // A header is text, so its value is checked as a string. Every header of our contracts is one.
                    String schema = "/paths/%s/%s/responses/%s/headers/%s/schema"
                            .formatted(segment(path), method, key, segment(name));
                    errorsOf(schema, JSON.writeValueAsString(value))
                            .forEach(error -> violations.add("The header %s: %s.".formatted(name, error.getMessage())));
                }
            });
            JsonNode content = response.path("content");
            boolean hasBody = body != null && !body.isEmpty();
            if (content.isEmpty()) {
                if (hasBody) {
                    violations.add("The contract gives this response no body, and it has one.");
                }
                return violations;
            }
            MediaType actual = headers.getContentType();
            Optional<String> declared = content.propertyNames().stream()
                    .filter(name ->
                            actual != null && MediaType.parseMediaType(name).equalsTypeAndSubtype(actual))
                    .findFirst();
            if (declared.isEmpty()) {
                violations.add("The media type is %s, and the contract has %s."
                        .formatted(actual == null ? "missing" : actual, String.join(" or ", content.propertyNames())));
                return violations;
            }
            if (!hasBody) {
                violations.add("The contract gives this response a body, and it has none.");
                return violations;
            }
            if (content.path(declared.get()).has("schema")) {
                String schema = "/paths/%s/%s/responses/%s/content/%s/schema"
                        .formatted(segment(path), method, key, segment(declared.get()));
                violations.addAll(bodyViolations(schema, body));
            }
            return violations;
        }
    }

    /** Checks a body against the schema at a pointer of the document. Each violation names its place in the body. */
    private List<String> bodyViolations(String schema, String body) {
        List<Error> errors;
        try {
            errors = errorsOf(schema, body);
        } catch (JacksonException notJson) {
            return List.of("The body is no JSON.");
        }
        return errors.stream()
                .map(error -> {
                    String place = error.getInstanceLocation().toString();
                    return "The body%s: %s.".formatted(place.isEmpty() ? "" : " at " + place, error.getMessage());
                })
                .toList();
    }

    private List<Error> errorsOf(String schema, String json) {
        return schemas.getSchema(SchemaLocation.of(location + "#" + schema))
                .validate(json, InputFormat.JSON, context -> context.setExecutionConfig(FORMATS_ARE_RULES));
    }

    /** One segment of a JSON Pointer, by the escapes of RFC 6901. */
    private static String segment(String name) {
        return name.replace("~", "~0").replace("/", "~1");
    }
}
