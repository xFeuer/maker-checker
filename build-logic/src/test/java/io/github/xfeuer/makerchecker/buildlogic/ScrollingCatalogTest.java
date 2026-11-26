package io.github.xfeuer.makerchecker.buildlogic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.api.GradleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScrollingCatalogTest {

    private static final String RULES_OF_VACUUM = ".pp-catalog-body {\n    margin: 0;\n}\n";

    private static final String THE_RULE =
            "html:has(body.pp-catalog-body),\nbody.pp-catalog-body {\n    height: auto;\n    overflow: visible;\n}";

    private static final String FIRST_PAGE = """
            <!DOCTYPE html>
            <html lang="en" theme="dark">
            <head>
            <link rel="stylesheet" href="static/printing-press.css">
            <link rel="stylesheet" href="static/printing-press-catalog.css">
            </head>
            <body class="pp-catalog-body">
            <main class="pp-catalog-shell"></main>
            </body>
            </html>
            """;

    @Test
    void addsARuleForThePagesOfTheCatalogAfterTheRulesOfVacuum(@TempDir Path reference) throws IOException {
        Path styleSheet = catalog(reference, FIRST_PAGE);

        ScrollingCatalog.in(reference);

        assertThat(Files.readString(styleSheet))
                .startsWith(RULES_OF_VACUUM)
                .contains(THE_RULE)
                .endsWith("}\n");
    }

    @Test
    void findsTheClassAmongOtherClassesAndAttributesOfTheBody(@TempDir Path reference) throws IOException {
        String firstPage = FIRST_PAGE.replace(
                "<body class=\"pp-catalog-body\">", "<body id=\"top\" class=\"wide pp-catalog-body\">");
        Path styleSheet = catalog(reference, firstPage);

        ScrollingCatalog.in(reference);

        assertThat(Files.readString(styleSheet)).contains(THE_RULE);
    }

    @Test
    void failsWhenVacuumNoLongerWritesTheStyleSheet(@TempDir Path reference) throws IOException {
        Files.writeString(reference.resolve("index.html"), FIRST_PAGE);

        assertThatThrownBy(() -> ScrollingCatalog.in(reference))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("vacuum wrote no style sheet static/printing-press-catalog.css.")
                .hasMessageContaining("change ScrollingCatalog or remove it");
    }

    @Test
    void failsWhenTheFirstPageNoLongerLoadsTheStyleSheet(@TempDir Path reference) throws IOException {
        String firstPage = FIRST_PAGE.replace("printing-press-catalog.css", "printing-press-index.css");
        Path styleSheet = catalog(reference, firstPage);

        assertThatThrownBy(() -> ScrollingCatalog.in(reference))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("its first page does not load the style sheet static/printing-press-catalog.css.")
                .hasMessageContaining("change ScrollingCatalog or remove it");
        assertThat(styleSheet).hasContent(RULES_OF_VACUUM);
    }

    @Test
    void failsWhenTheBodyOfTheFirstPageNoLongerHasTheClass(@TempDir Path reference) throws IOException {
        // The old name at the start of a new one is not the class.
        String firstPage = FIRST_PAGE.replace("\"pp-catalog-body\"", "\"pp-catalog-body-2\"");
        Path styleSheet = catalog(reference, firstPage);

        assertThatThrownBy(() -> ScrollingCatalog.in(reference))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("the body of its first page does not have the class pp-catalog-body.")
                .hasMessageContaining("change ScrollingCatalog or remove it");
        assertThat(styleSheet).hasContent(RULES_OF_VACUUM);
    }

    /** A reference with this first page and with the style sheet of the catalog, as vacuum writes them. */
    private static Path catalog(Path reference, String firstPage) throws IOException {
        Files.writeString(reference.resolve("index.html"), firstPage);
        Path styleSheet = reference.resolve(ScrollingCatalog.STYLE_SHEET);
        Files.createDirectories(styleSheet.getParent());
        Files.writeString(styleSheet, RULES_OF_VACUUM);
        return styleSheet;
    }
}
