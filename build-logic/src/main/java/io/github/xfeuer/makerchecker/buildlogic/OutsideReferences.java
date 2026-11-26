package io.github.xfeuer.makerchecker.buildlogic;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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

    /**
     * The same for every page under a directory, however deep. Each element names its page by the path from the
     * directory, with forward slashes, and then the tag.
     */
    static List<String> under(Path directory) throws IOException {
        try (Stream<Path> files = Files.walk(directory)) {
            return files.filter(file -> file.getFileName().toString().endsWith(".html"))
                    .sorted()
                    .flatMap(page -> in(read(page)).stream().map(tag -> "%s: %s".formatted(name(directory, page), tag)))
                    .toList();
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private static String read(Path page) {
        try {
            return Files.readString(page);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String name(Path directory, Path page) {
        return directory.relativize(page).toString().replace(File.separatorChar, '/');
    }
}
