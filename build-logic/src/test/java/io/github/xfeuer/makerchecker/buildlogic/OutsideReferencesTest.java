package io.github.xfeuer.makerchecker.buildlogic;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OutsideReferencesTest {

    @Test
    void findsAScriptThatComesFromAnotherHost() {
        String html = """
                <body>
                <script async src="https://www.googletagmanager.com/gtag/js?id=G-1"></script>
                </body>
                """;

        assertThat(OutsideReferences.in(html))
                .containsExactly("<script async src=\"https://www.googletagmanager.com/gtag/js?id=G-1");
    }

    @Test
    void findsAStyleSheetAnImageAndAFrameWhateverTheSchemeAndTheQuotes() {
        String html = """
                <link rel="stylesheet" href="//cdn.example/app.css">
                <IMG SRC='http://tracker.example/pixel.gif'>
                <iframe src=https://example.org/frame></iframe>
                """;

        assertThat(OutsideReferences.in(html))
                .containsExactly(
                        "<link rel=\"stylesheet\" href=\"//cdn.example/app.css",
                        "<IMG SRC='http://tracker.example/pixel.gif",
                        "<iframe src=https://example.org/frame");
    }

    @Test
    void acceptsAPageThatCarriesEverythingItself() {
        String html = """
                <script>window.report = {};</script>
                <script src="app.js"></script>
                <link rel="icon" href="data:image/png;base64,AAAA">
                <img src="/images/logo.png">
                <a href="https://allurereport.org">Allure Report</a>
                """;

        assertThat(OutsideReferences.in(html)).isEmpty();
    }

    @Test
    void findsThemInEveryPageUnderADirectoryAndNamesThePage(@TempDir Path site) throws IOException {
        Files.createDirectories(site.resolve("services/payments/operations"));
        Files.writeString(site.resolve("index.html"), "<script src=\"static/app.js\"></script>");
        Files.writeString(
                site.resolve("services/payments/operations/create-payment.html"),
                "<link rel=\"stylesheet\" href=\"https://fonts.example/css\">\n<img src=\"//cdn.example/logo.png\">");
        Files.writeString(site.resolve("services/payments/about.html"), "<script src=\"https://cdn.example/a.js\">");
        // No browser reads this one as a page.
        Files.writeString(site.resolve("services/payments/data.js"), "<script src=\"https://cdn.example/b.js\">");

        assertThat(OutsideReferences.under(site))
                .containsExactly(
                        "services/payments/about.html: <script src=\"https://cdn.example/a.js",
                        "services/payments/operations/create-payment.html: "
                                + "<link rel=\"stylesheet\" href=\"https://fonts.example/css",
                        "services/payments/operations/create-payment.html: <img src=\"//cdn.example/logo.png");
    }

    @Test
    void findsNothingUnderADirectoryWhosePagesCarryEverythingThemselves(@TempDir Path site) throws IOException {
        Files.writeString(site.resolve("index.html"), "<a href=\"https://example.org\">A link</a>");

        assertThat(OutsideReferences.under(site)).isEmpty();
    }
}
