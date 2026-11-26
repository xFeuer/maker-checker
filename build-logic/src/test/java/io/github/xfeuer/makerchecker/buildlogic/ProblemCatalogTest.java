package io.github.xfeuer.makerchecker.buildlogic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import groovy.json.JsonSlurper;
import io.github.xfeuer.makerchecker.buildlogic.ProblemType.Example;
import io.github.xfeuer.makerchecker.buildlogic.ProblemType.Occurrence;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProblemCatalogTest {

    private static final String PLACE = "https://example.org/problems/";

    @Test
    void collectsATypeWithItsStatusItsTitleAndTheOperationsThatAnswerWithIt() {
        Object payments = specification("payment-service", """
                "/v1/payments": {
                  "post": {
                    "summary": "Create a payment.",
                    "responses": {
                      "201": {"content": {"application/json": {"examples": {"created": {"value": {"state": "SETTLED"}}}}}},
                      "422": {"content": {"application/problem+json": {"examples": {
                        "paymentLimitExceeded": {
                          "summary": "An amount above the payment limit",
                          "description": "The problem of LIM-2.",
                          "value": {"type": "%spayment-limit-exceeded", "title": "The amount is above the limit.", "status": 422}
                        }
                      }}}}
                    }
                  }
                }
                """.formatted(PLACE));

        assertThat(ProblemCatalog.in(List.of(payments)))
                .containsExactly(new ProblemType(
                        "payment-limit-exceeded",
                        PLACE + "payment-limit-exceeded",
                        422,
                        "The amount is above the limit.",
                        List.of(new Example("An amount above the payment limit", "The problem of LIM-2.")),
                        List.of(new Occurrence("payment-service", "POST", "/v1/payments", "Create a payment."))));
    }

    @Test
    void ordersTheTypesByNameAndTheOperationsBySpecificationPathAndMethod() {
        Object payments = specification("payment-service", """
                "/v1/payments/{paymentId}": {
                  "put": {"summary": "Change a payment.", "responses": {"403": %1$s, "404": %2$s}},
                  "get": {"summary": "Read a payment.", "responses": {"403": %1$s}}
                },
                "/v1/payments": {
                  "get": {"summary": "List the payments.", "responses": {"403": %1$s}}
                }
                """.formatted(
                        response("insufficient-scope", 403), response("a-missing-thing", 404)));
        Object accounts = specification("account-service", """
                "/v1/accounts": {
                  "get": {"summary": "List the accounts.", "responses": {"403": %s}}
                }
                """.formatted(response("insufficient-scope", 403)));

        List<ProblemType> problems = ProblemCatalog.in(List.of(payments, accounts));

        assertThat(problems).extracting(ProblemType::name).containsExactly("a-missing-thing", "insufficient-scope");
        assertThat(problems.get(1).occurrences())
                .containsExactly(
                        new Occurrence("account-service", "GET", "/v1/accounts", "List the accounts."),
                        new Occurrence("payment-service", "GET", "/v1/payments", "List the payments."),
                        new Occurrence("payment-service", "GET", "/v1/payments/{paymentId}", "Read a payment."),
                        new Occurrence("payment-service", "PUT", "/v1/payments/{paymentId}", "Change a payment."));
    }

    @Test
    void namesAnExampleOnceHoweverManyOperationsShowIt() {
        Object payments = specification("payment-service", """
                "/v1/payments": {
                  "get": {"summary": "List the payments.", "responses": {"403": %1$s}},
                  "post": {"summary": "Create a payment.", "responses": {"403": %1$s}}
                }
                """.formatted(response("insufficient-scope", 403)));

        assertThat(ProblemCatalog.in(List.of(payments)))
                .singleElement()
                .extracting(ProblemType::examples)
                .isEqualTo(List.of(new Example("An example of insufficient-scope", "")));
    }

    @Test
    void leavesOutAProblemWhoseStatusCodeSaysEverything() {
        Object accounts = specification("account-service", """
                "/v1/accounts": {
                  "get": {"summary": "List the accounts.", "responses": {"401": {"content": {"application/problem+json": {"examples": {
                    "unauthenticated": {"summary": "No token", "value": {"type": "about:blank", "title": "Unauthorized", "status": 401}}
                  }}}}}}
                }
                """);

        assertThat(ProblemCatalog.in(List.of(accounts))).isEmpty();
    }

    @Test
    void followsAReferenceThatStaysInsideTheSpecification() {
        Object accounts = new JsonSlurper().parseText("""
                {
                  "info": {"title": "account-service"},
                  "paths": {
                    "/v1/holds/{holdId}": {"put": {"summary": "Place a hold.", "responses": {"409": {"$ref": "#/components/responses/Conflict"}}}}
                  },
                  "components": {
                    "responses": {"Conflict": {"content": {"application/problem+json": {"examples": {
                      "holdExists": {"$ref": "#/components/examples/HoldExists"}
                    }}}}},
                    "examples": {"HoldExists": {
                      "summary": "A second hold under one identifier",
                      "value": {"type": "%shold-exists", "title": "A hold with this identifier exists.", "status": 409}
                    }}
                  }
                }
                """.formatted(PLACE));

        assertThat(ProblemCatalog.in(List.of(accounts))).singleElement().satisfies(problem -> {
            assertThat(problem.name()).isEqualTo("hold-exists");
            assertThat(problem.occurrences())
                    .containsExactly(new Occurrence("account-service", "PUT", "/v1/holds/{holdId}", "Place a hold."));
        });
    }

    @Test
    void failsWhenTheSpecificationsGiveOneTypeTwoTitlesOrTwoStatuses() {
        Object payments = specification("payment-service", """
                "/v1/payments": {"post": {"summary": "Create a payment.", "responses": {"409": %s}}}
                """.formatted(response("payment-exists", 409)));
        Object network = specification("clearing-network", """
                "/v1/payments/{endToEndId}": {"put": {"summary": "Send a payment.", "responses": {"422": {"content": {"application/problem+json": {"examples": {
                  "paymentExists": {"summary": "A second payment", "value": {"type": "%spayment-exists", "title": "The payment exists.", "status": 422}}
                }}}}}}}
                """.formatted(PLACE));

        assertThatThrownBy(() -> ProblemCatalog.in(List.of(payments, network)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        PLACE + "payment-exists has more than one title: The payment exists. | payment-exists.")
                .hasMessageContaining(PLACE + "payment-exists has more than one status: [409, 422]");
    }

    @Test
    void failsWhenATypeDoesNotEndInANameThatCanBeAFileName() {
        Object payments = specification("payment-service", """
                "/v1/payments": {"post": {"summary": "Create a payment.", "responses": {"409": {"content": {"application/problem+json": {"examples": {
                  "paymentExists": {"summary": "A second payment", "value": {"type": "%s..%%2Fpayment_exists", "title": "The payment exists.", "status": 409}}
                }}}}}}}
                """.formatted(PLACE));

        assertThatThrownBy(() -> ProblemCatalog.in(List.of(payments)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("POST /v1/payments of payment-service has an example of a problem whose type does not"
                        + " end in a name in kebab-case: " + PLACE + "..%2Fpayment_exists");
    }

    @Test
    void failsWhenTheTypesLieAtMoreThanOneAddress() {
        Object payments = specification("payment-service", """
                "/v1/payments": {"post": {"summary": "Create a payment.", "responses": {"409": %s, "422": {"content": {"application/problem+json": {"examples": {
                  "unknownAlias": {"summary": "An alias that nobody knows", "value": {"type": "https://example.com/errors/unknown-alias", "title": "The alias is unknown.", "status": 422}}
                }}}}}}}
                """.formatted(response("payment-exists", 409)));

        assertThatThrownBy(() -> ProblemCatalog.in(List.of(payments)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The problem types lie at more than one address: https://example.com/errors/ | " + PLACE);
    }

    /** A bundled specification with these path items. */
    private static Object specification(String title, String paths) {
        return new JsonSlurper().parseText("""
                {"info": {"title": "%s"}, "paths": {%s}}
                """.formatted(title, paths));
    }

    /** An error response with one example of a problem. The title of the problem is its name with a period. */
    private static String response(String name, int status) {
        return """
                {"content": {"application/problem+json": {"examples": {"example": {
                  "summary": "An example of %1$s",
                  "value": {"type": "%2$s%1$s", "title": "%1$s.", "status": %3$d}
                }}}}}
                """.formatted(name, PLACE, status);
    }
}
