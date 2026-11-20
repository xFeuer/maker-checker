package io.github.xfeuer.makerchecker.buildlogic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

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
}
