package io.github.xfeuer.makerchecker.buildlogic;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.xfeuer.makerchecker.buildlogic.ProblemType.Example;
import io.github.xfeuer.makerchecker.buildlogic.ProblemType.Occurrence;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProblemHtmlTest {

    private static final ProblemType LIMIT = new ProblemType(
            "payment-limit-exceeded",
            "https://example.org/problems/payment-limit-exceeded",
            422,
            "The amount is above the payment limit of the account.",
            List.of(
                    new Example("An amount above the payment limit", "The problem of LIM-2."),
                    new Example("An amount far above the payment limit", "")),
            List.of(new Occurrence("payment-service", "POST", "/v1/payments", "Create a payment.")));

    private static final ProblemType SCOPE = new ProblemType(
            "insufficient-scope",
            "https://example.org/problems/insufficient-scope",
            403,
            "The access token lacks a scope that the operation needs.",
            List.of(new Example("A token without a scope", "")),
            List.of(new Occurrence("account-service", "GET", "/v1/accounts", "List the accounts.")));

    private final ProblemHtml html = new ProblemHtml("maker-checker API reference");

    @Test
    void namesThePageOfATypeAfterTheType() {
        assertThat(ProblemHtml.fileName(LIMIT)).isEqualTo("payment-limit-exceeded.html");
    }

    @Test
    void showsWhatAClientReliesOnAndWhereTheProblemComesFrom() {
        assertThat(html.page(LIMIT))
                .startsWith("<!DOCTYPE html>")
                .contains("<title>payment-limit-exceeded: a problem type</title>")
                .contains("<h1>payment-limit-exceeded</h1>")
                .contains("<p class=\"lead\">The amount is above the payment limit of the account.</p>")
                .contains("<dt>Status</dt><dd>422</dd>")
                .contains("<dt>Type</dt><dd><code>https://example.org/problems/payment-limit-exceeded</code></dd>")
                .contains("<li><strong>An amount above the payment limit</strong><br>The problem of LIM-2.</li>")
                .contains("<li><strong>An amount far above the payment limit</strong></li>")
                .contains("<tr><td class=\"name\">payment-service</td><td><code>POST /v1/payments</code></td>"
                        + "<td>Create a payment.</td></tr>");
    }

    @Test
    void linksAPageToTheListAndToTheReferenceNextToIt() {
        assertThat(html.page(LIMIT))
                .contains("<a href=\"../api/index.html\">maker-checker API reference</a>")
                .contains("<a href=\"index.html\">Problem types</a>");
    }

    @Test
    void listsEveryTypeWithALinkToItsPage() {
        assertThat(html.index(List.of(SCOPE, LIMIT)))
                .contains("<title>Problem types</title>")
                .contains("<a href=\"../api/index.html\">maker-checker API reference</a>")
                .containsSubsequence(
                        "<a href=\"insufficient-scope.html\"><code>insufficient-scope</code></a></td><td>403</td>"
                                + "<td>The access token lacks a scope that the operation needs.</td>",
                        "<a href=\"payment-limit-exceeded.html\"><code>payment-limit-exceeded</code></a></td>"
                                + "<td>422</td><td>The amount is above the payment limit of the account.</td>");
    }

    @Test
    void showsTheTextOfASpecificationAsTextAndNeverAsMarkup() {
        ProblemType hostile = new ProblemType(
                "hostile",
                "https://example.org/problems/hostile",
                400,
                "<script src=\"https://example.com/x.js\"></script>",
                List.of(new Example("Tom & Jerry's <b>", "\"quoted\"")),
                List.of(new Occurrence("a<b", "GET", "/v1/<things>", "Read & write.")));

        String page = new ProblemHtml("R&D reference").page(hostile);

        assertThat(page)
                .contains("&lt;script src=&quot;https://example.com/x.js&quot;&gt;&lt;/script&gt;")
                .contains("<strong>Tom &amp; Jerry&#39;s &lt;b&gt;</strong><br>&quot;quoted&quot;")
                .contains("<td class=\"name\">a&lt;b</td><td><code>GET /v1/&lt;things&gt;</code></td>"
                        + "<td>Read &amp; write.</td>")
                .contains(">R&amp;D reference</a>")
                .doesNotContain("<script");
        assertThat(OutsideReferences.in(page)).isEmpty();
    }

    @Test
    void loadsNothingFromAnotherHost() {
        assertThat(OutsideReferences.in(html.page(LIMIT))).isEmpty();
        assertThat(OutsideReferences.in(html.index(List.of(SCOPE, LIMIT)))).isEmpty();
    }
}
