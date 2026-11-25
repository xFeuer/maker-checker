package io.github.xfeuer.makerchecker.buildlogic;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.gradle.api.GradleException;

/** The SHA-256 of a file, and the one that the checksums file of a release states for it. */
final class Sha256 {

    private Sha256() {}

    /** The SHA-256 of the content of a file, in lower case. */
    static String of(Path file) throws IOException {
        MessageDigest digest = digest();
        try (InputStream content = new DigestInputStream(Files.newInputStream(file), digest)) {
            content.transferTo(OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /**
     * The SHA-256 that a checksums file states for the file with the given name, in lower case. It is empty when no
     * line names the file.
     *
     * <p>The file is the one that {@code sha256sum} writes: each line holds a SHA-256 and a name.
     */
    static Optional<String> statedFor(String name, String checksums) {
        List<String> stated = checksums
                .lines()
                .map(line -> line.strip().split("\\s+", 2))
                // sha256sum puts a star before the name of a file that it read in binary mode.
                .filter(fields ->
                        fields.length == 2 && fields[1].replaceFirst("^\\*", "").equals(name))
                .map(fields -> fields[0].toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
        if (stated.size() > 1) {
            throw new GradleException("The checksums file states more than one SHA-256 for %s.".formatted(name));
        }
        return stated.stream().findFirst();
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Every Java platform has SHA-256.", e);
        }
    }
}
