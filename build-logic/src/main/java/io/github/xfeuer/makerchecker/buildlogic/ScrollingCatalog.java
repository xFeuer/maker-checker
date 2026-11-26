package io.github.xfeuer.makerchecker.buildlogic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.gradle.api.GradleException;

/**
 * Lets the pages of the catalog scroll. See ADR 0015.
 *
 * <p>vacuum 0.43.0 keeps every page of the reference from scrolling as a whole. The page of a specification scrolls
 * inside a panel instead. The pages of the catalog have no such panel, so in a small window their lower part cannot
 * be reached. One more rule at the end of their style sheet lets those pages scroll.
 *
 * <p>The rule works as long as a page of the catalog loads that style sheet and has the class of the rule on its
 * body. The build fails when the first page of the catalog does not. It sees no other change of those pages. The
 * rule can go when vacuum lets its catalog scroll.
 */
final class ScrollingCatalog {

    /** Only the pages of the catalog load this style sheet. The path is the one from the root of the reference. */
    static final String STYLE_SHEET = "static/printing-press-catalog.css";

    // A page of the catalog has this class on its body. The rule names it, so it holds for no other page.
    private static final String CLASS_OF_THE_BODY = "pp-catalog-body";

    // The first page of the catalog, at the root of the reference.
    private static final String FIRST_PAGE = "index.html";

    private static final Pattern CLASSES_OF_THE_BODY = Pattern.compile("<body\\b[^>]*\\sclass=\"([^\"]*)\"");

    // The page gets the height of its content and is not clipped, so the window scrolls it. The header of vacuum
    // sticks to the top of the window only in a body of that height.
    private static final String RULE = """

            /* Added by the build of this repository: vacuum keeps every page from scrolling as a whole, and a page
             * of the catalog has no panel that scrolls in its place. */
            html:has(body.%1$s),
            body.%1$s {
                height: auto;
                overflow: visible;
            }
            """.formatted(CLASS_OF_THE_BODY);

    private ScrollingCatalog() {}

    /** Adds the rule to the style sheet of the catalog in this reference. */
    static void in(Path reference) throws IOException {
        Path styleSheet = reference.resolve(STYLE_SHEET);
        if (!Files.isRegularFile(styleSheet)) {
            throw unexpected("vacuum wrote no style sheet %s".formatted(STYLE_SHEET));
        }
        String firstPage = Files.readString(reference.resolve(FIRST_PAGE));
        if (!firstPage.contains("href=\"%s\"".formatted(STYLE_SHEET))) {
            throw unexpected("its first page does not load the style sheet %s".formatted(STYLE_SHEET));
        }
        if (!classesOfTheBody(firstPage).contains(CLASS_OF_THE_BODY)) {
            throw unexpected("the body of its first page does not have the class %s".formatted(CLASS_OF_THE_BODY));
        }
        Files.writeString(styleSheet, Files.readString(styleSheet) + RULE);
    }

    private static List<String> classesOfTheBody(String page) {
        Matcher body = CLASSES_OF_THE_BODY.matcher(page);
        return body.find() ? List.of(body.group(1).trim().split("\\s+")) : List.of();
    }

    private static GradleException unexpected(String finding) {
        return new GradleException(("The catalog of vacuum is not what the build expects: %s. The build adds a rule"
                        + " to the style sheet %s for the pages with the class %s, because the catalog of vacuum"
                        + " 0.43.0 cannot scroll. Look at the catalog of this vacuum in a small window, then change"
                        + " ScrollingCatalog or remove it.")
                .formatted(finding, STYLE_SHEET, CLASS_OF_THE_BODY));
    }
}
