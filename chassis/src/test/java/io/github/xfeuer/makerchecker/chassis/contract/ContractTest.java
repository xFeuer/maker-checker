package io.github.xfeuer.makerchecker.chassis.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** The contract of the test application: a bundle as the build makes one, written by hand. */
class ContractTest {

    private final Contract contract = Contract.ofResource("contracts/openapi.json");

    @Test
    void namesItsOperationsByMethodAndPath() {
        assertThat(contract.operations())
                .containsExactlyInAnyOrder(
                        "GET /works",
                        "GET /breaks-the-contract",
                        "GET /cups/{number}",
                        "GET /cups/latest",
                        "POST /orders");
    }

    @Test
    void findsTheOperationOfARequest() {
        assertThat(contract.operationFor("GET", "/cups/7"))
                .map(Contract.Operation::name)
                .contains("GET /cups/{number}");
        assertThat(contract.operationFor("get", "/works"))
                .map(Contract.Operation::name)
                .contains("GET /works");
    }

    @Test
    void takesAPathWithoutAParameterBeforeOneWithAParameter() {
        assertThat(contract.operationFor("GET", "/cups/latest"))
                .map(Contract.Operation::name)
                .contains("GET /cups/latest");
    }

    @Test
    void hasNoOperationForAnotherPathOrAnotherMethod() {
        assertThat(contract.operationFor("GET", "/nowhere")).isEmpty();
        assertThat(contract.operationFor("GET", "/cups/7/saucers")).isEmpty();
        assertThat(contract.operationFor("DELETE", "/works")).isEmpty();
    }

    @Test
    void takesAResponseThatKeepsTheContract() {
        assertThat(violations("/works", 200, json(), "{\"answer\": \"yes\"}")).isEmpty();
    }

    @Test
    void takesAMediaTypeWithAParameter() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/json;charset=UTF-8"));

        assertThat(violations("/works", 200, headers, "{\"answer\": \"yes\"}")).isEmpty();
    }

    @Test
    void namesEveryViolationOfABodyWithItsPlace() {
        assertThat(violations("/works", 200, json(), "{\"answer\": 5, \"kettle\": \"hot\"}"))
                .hasSize(2)
                .anySatisfy(
                        violation -> assertThat(violation).contains("/answer").contains("string"))
                .anySatisfy(violation -> assertThat(violation).contains("kettle"));
    }

    @Test
    void holdsAFormatAsARule() {
        String body = "{\"cupId\": \"not-a-uuid\", \"pouredAt\": \"2026-11-26T10:00:00\"}";

        assertThat(violations("/cups/latest", 200, json(), body))
                .hasSize(2)
                .anySatisfy(violation -> assertThat(violation).contains("/cupId"))
                .anySatisfy(violation -> assertThat(violation).contains("/pouredAt"));
    }

    @Test
    void refusesAMediaTypeThatTheContractDoesNotHave() {
        HttpHeaders text = new HttpHeaders();
        text.setContentType(MediaType.TEXT_PLAIN);
        HttpHeaders problem = new HttpHeaders();
        problem.setContentType(MediaType.APPLICATION_PROBLEM_JSON);

        assertThat(violations("/works", 200, text, "yes"))
                .containsExactly("The media type is text/plain, and the contract has application/json.");
        assertThat(violations("/works", 200, problem, "{\"answer\": \"yes\"}"))
                .containsExactly("The media type is application/problem+json, and the contract has application/json.");
    }

    @Test
    void asksForAHeaderThatTheContractRequires() {
        assertThat(violations("/breaks-the-contract", 200, json(), "{\"answer\": \"yes\"}"))
                .containsExactly("The header X-Kettle is required, and the response has none.");
    }

    @Test
    void takesAHeaderThatKeepsItsSchema() {
        HttpHeaders headers = json();
        headers.set("X-Kettle", "hot");

        assertThat(violations("/breaks-the-contract", 200, headers, "{\"answer\": \"yes\"}"))
                .isEmpty();
    }

    @Test
    void holdsTheValueOfAHeaderToItsSchema() {
        HttpHeaders headers = json();
        headers.set("X-Kettle", "cold");

        assertThat(violations("/breaks-the-contract", 200, headers, "{\"answer\": \"yes\"}"))
                .singleElement()
                .asString()
                .startsWith("The header X-Kettle: ")
                .contains("warm");
    }

    @Test
    void takesTheDefaultResponseForAStatusWithoutAResponseOfItsOwn() {
        HttpHeaders problem = new HttpHeaders();
        problem.setContentType(MediaType.APPLICATION_PROBLEM_JSON);

        assertThat(violations(
                        "/works",
                        404,
                        problem,
                        "{\"type\": \"about:blank\", \"title\": \"Not Found\", \"status\": 404}"))
                .isEmpty();
        assertThat(violations("/works", 404, problem, "{\"timestamp\": \"now\", \"status\": 404}"))
                .isNotEmpty();
    }

    @Test
    void refusesAStatusThatTheOperationDoesNotHave() {
        assertThat(violations("/cups/latest", 500, json(), "{}"))
                .containsExactly("The contract has no response with the status 500.");
    }

    @Test
    void refusesABodyThatIsNoJson() {
        assertThat(violations("/works", 200, json(), "{\"answer\": ")).containsExactly("The body is no JSON.");
    }

    @Test
    void refusesAResponseWithoutTheBodyThatTheContractGivesIt() {
        assertThat(violations("/works", 200, json(), ""))
                .containsExactly("The contract gives this response a body, and it has none.");
    }

    @Test
    void refusesABodyWhereTheContractHasNone() {
        assertThat(violations("/cups/7", 204, new HttpHeaders(), "{}"))
                .containsExactly("The contract gives this response no body, and it has one.");
        assertThat(violations("/cups/7", 204, new HttpHeaders(), "")).isEmpty();
    }

    @Test
    void listsTheTypesOfProblemThatItsExamplesShow() {
        assertThat(contract.problemTypes())
                .containsExactly(new Contract.ProblemExample(
                        URI.create("https://xfeuer.github.io/maker-checker/problems/teapot-empty"),
                        422,
                        "The teapot is empty."));
    }

    @Test
    void refusesADocumentWithAReferenceOutsideItsSchemas() {
        String document = """
            {"paths": {"/works": {"get": {"responses": {"default": {"$ref": "#/components/responses/Problem"}}}}}}
            """;

        assertThatThrownBy(() -> new Contract("classpath:contracts/not-a-bundle.json", document))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("contracts/not-a-bundle.json")
                .hasMessageContaining("/paths/~1works/get/responses/default")
                .hasMessageContaining("#/components/responses/Problem");
    }

    @Test
    void saysWhereTheBundleShouldBeWhenItIsMissing() {
        assertThatThrownBy(() -> Contract.ofResource("contracts/missing.json"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("contracts/missing.json")
                .hasMessageContaining("docs/adr/0019");
    }

    private List<String> violations(String path, int status, HttpHeaders headers, String body) {
        return contract.operationFor("GET", path).orElseThrow().violationsOf(status, headers, body);
    }

    private static HttpHeaders json() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
