package io.github.xfeuer.makerchecker.buildlogic;

import io.github.xfeuer.makerchecker.buildlogic.ProblemType.Example;
import io.github.xfeuer.makerchecker.buildlogic.ProblemType.Occurrence;
import java.util.List;

/**
 * Writes the pages of the problem types: one page for each type, and one that lists them all. See ADR 0015.
 *
 * <p>A page carries its style itself and loads nothing. Its links are relative: to the other pages, which lie in
 * the same directory, and to the API reference, which lies next to that directory under {@code api}.
 *
 * @param referenceTitle the title of the API reference, for the link that leads there
 */
record ProblemHtml(String referenceTitle) {

    private static final String REFERENCE = "../api/index.html";
    private static final String LIST = "index.html";

    private static final String STYLE = """
            :root {
              color-scheme: light dark;
              --paper: #f7f8fa;
              --ink: #18212c;
              --muted: #5a6675;
              --rule: #d0d7e0;
              --accent: #0b6aa2;
              --code: #e9edf2;
            }
            @media (prefers-color-scheme: dark) {
              :root {
                --paper: #10151c;
                --ink: #e4e9ef;
                --muted: #9aa7b6;
                --rule: #2b3643;
                --accent: #6cc0f2;
                --code: #1b2430;
              }
            }
            * { box-sizing: border-box; }
            body {
              margin: 0;
              padding: 32px 20px 56px;
              background: var(--paper);
              color: var(--ink);
              font: 16px/1.55 system-ui, "Segoe UI", "Helvetica Neue", Arial, sans-serif;
            }
            main { max-width: 860px; margin: 0 auto; }
            a { color: var(--accent); }
            h1 { font-size: 28px; line-height: 1.2; margin: 8px 0; overflow-wrap: anywhere; }
            h2 { font-size: 18px; margin: 32px 0 8px; }
            code {
              font-family: ui-monospace, "Cascadia Mono", Consolas, "Liberation Mono", Menlo, monospace;
              font-size: 14px;
              background: var(--code);
              padding: 1px 5px;
              border-radius: 3px;
              overflow-wrap: anywhere;
            }
            ul { margin: 0; padding-left: 20px; }
            li { margin: 0 0 8px; }
            dl { display: grid; grid-template-columns: max-content 1fr; gap: 6px 16px; margin: 0; }
            dt { color: var(--muted); }
            dd { margin: 0; }
            table { border-collapse: collapse; width: 100%; }
            th, td { text-align: left; padding: 6px 16px 6px 0; border-bottom: 1px solid var(--rule); vertical-align: top; }
            th { color: var(--muted); font-size: 14px; font-weight: 600; }
            .lead { font-size: 18px; margin: 0 0 20px; }
            .crumbs, .note { color: var(--muted); font-size: 14px; }
            .note { margin-top: 32px; }
            .wide { overflow-x: auto; }
            .name { white-space: nowrap; }
            """;

    /** The name of the file that holds the page of a type. GitHub Pages also serves it without the ending. */
    static String fileName(ProblemType problem) {
        return problem.name() + ".html";
    }

    /** The page of one problem type. */
    String page(ProblemType problem) {
        StringBuilder body = new StringBuilder();
        body.append("<p class=\"crumbs\">%s / <a href=\"%s\">Problem types</a></p>\n".formatted(reference(), LIST));
        body.append("<h1>%s</h1>\n".formatted(escaped(problem.name())));
        body.append("<p class=\"lead\">%s</p>\n".formatted(escaped(problem.title())));
        body.append("<dl>\n");
        body.append("<dt>Status</dt><dd>%d</dd>\n".formatted(problem.status()));
        body.append("<dt>Type</dt><dd><code>%s</code></dd>\n".formatted(escaped(problem.type())));
        body.append("</dl>\n");

        body.append("<h2>Examples in the contracts</h2>\n<ul>\n");
        for (Example example : problem.examples()) {
            body.append("<li><strong>%s</strong>".formatted(escaped(example.summary())));
            if (!example.description().isEmpty()) {
                body.append("<br>").append(escaped(example.description()));
            }
            body.append("</li>\n");
        }
        body.append("</ul>\n");

        body.append("<h2>Operations that answer with it</h2>\n<div class=\"wide\">\n<table>\n");
        body.append("<thead><tr><th>Specification</th><th>Operation</th><th>Summary</th></tr></thead>\n<tbody>\n");
        for (Occurrence occurrence : problem.occurrences()) {
            body.append("<tr><td class=\"name\">%s</td><td><code>%s %s</code></td><td>%s</td></tr>\n"
                    .formatted(
                            escaped(occurrence.specification()),
                            escaped(occurrence.method()),
                            escaped(occurrence.path()),
                            escaped(occurrence.summary())));
        }
        body.append("</tbody>\n</table>\n</div>\n");

        body.append("<p class=\"note\">A response with this problem is a problem document of RFC 9457.");
        body.append(" A client relies on its type and its status. The title is for people.</p>\n");
        return document(problem.name() + ": a problem type", body);
    }

    /** The page that lists every problem type. */
    String index(List<ProblemType> problems) {
        StringBuilder body = new StringBuilder();
        body.append("<p class=\"crumbs\">%s</p>\n".formatted(reference()));
        body.append("<h1>Problem types</h1>\n");
        body.append("<p class=\"lead\">An error response of the contracts names the type of its problem.");
        body.append(" The type is the address of a page here.</p>\n");
        body.append("<div class=\"wide\">\n<table>\n");
        body.append("<thead><tr><th>Type</th><th>Status</th><th>Title</th></tr></thead>\n<tbody>\n");
        for (ProblemType problem : problems) {
            body.append("<tr><td class=\"name\"><a href=\"%s\"><code>%s</code></a></td><td>%d</td><td>%s</td></tr>\n"
                    .formatted(
                            escaped(fileName(problem)),
                            escaped(problem.name()),
                            problem.status(),
                            escaped(problem.title())));
        }
        body.append("</tbody>\n</table>\n</div>\n");
        body.append("<p class=\"note\">A problem whose status code says everything has the type");
        body.append(" <code>about:blank</code> and no page.</p>\n");
        return document("Problem types", body);
    }

    private String reference() {
        return "<a href=\"%s\">%s</a>".formatted(REFERENCE, escaped(referenceTitle));
    }

    private static String document(String title, CharSequence body) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>%s</title>
                <style>
                %s</style>
                </head>
                <body>
                <main>
                %s</main>
                </body>
                </html>
                """.formatted(escaped(title), STYLE, body);
    }

    /** The text as HTML shows it: a specification can hold any character. */
    static String escaped(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
