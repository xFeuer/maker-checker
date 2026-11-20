package io.github.xfeuer.makerchecker.buildlogic;

import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/** Finds the tags of an HTML page that load something from another host. */
final class OutsideReferences {

    // A script, a style sheet, an image or a frame whose address starts with a host. A link that a reader has to
    // click is not among them: it loads nothing on its own.
    private static final Pattern LOADING_TAG = Pattern.compile(
            "<(?:script|link|img|iframe)\\b[^>]*\\b(?:src|href)\\s*=\\s*[\"']?(?:https?:)?//[^\"'\\s>]*",
            Pattern.CASE_INSENSITIVE);

    private OutsideReferences() {}

    /** The start of each such tag, up to the end of its address. */
    static List<String> in(String html) {
        return LOADING_TAG.matcher(html).results().map(MatchResult::group).toList();
    }
}
