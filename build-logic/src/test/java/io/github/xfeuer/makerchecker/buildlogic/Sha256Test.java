package io.github.xfeuer.makerchecker.buildlogic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.api.GradleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Sha256Test {

    private static final String OF_ABC = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
    private static final String OF_NOTHING = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    @TempDir
    Path directory;

    @Test
    void hashesTheContentOfAFile() throws IOException {
        Path file = Files.writeString(directory.resolve("file"), "abc");

        assertThat(Sha256.of(file)).isEqualTo(OF_ABC);
    }

    @Test
    void findsTheLineOfAFileAmongTheOthers() {
        String checksums = """
                %s  vacuum_0.43.0_darwin_arm64.tar.gz
                %s  vacuum_0.43.0_linux_x86_64.tar.gz
                """.formatted(OF_NOTHING, OF_ABC);

        assertThat(Sha256.statedFor("vacuum_0.43.0_linux_x86_64.tar.gz", checksums))
                .contains(OF_ABC);
    }

    @Test
    void readsALineWhateverItsCaseItsLineEndingAndItsBinaryMark() {
        String checksums = "%s *archive.tar.gz\r\n".formatted(OF_ABC.toUpperCase());

        assertThat(Sha256.statedFor("archive.tar.gz", checksums)).contains(OF_ABC);
    }

    @Test
    void statesNothingForAFileThatNoLineNames() {
        String checksums = "%s  vacuum_0.43.0_linux_x86_64.tar.gz\n".formatted(OF_ABC);

        assertThat(Sha256.statedFor("vacuum_0.44.0_linux_x86_64.tar.gz", checksums))
                .isEmpty();
        assertThat(Sha256.statedFor("vacuum_0.43.0_linux_x86_64.tar.gz", "")).isEmpty();
    }

    @Test
    void rejectsAFileThatStatesTwoChecksumsForOneName() {
        String checksums = """
                %s  archive.tar.gz
                %s  archive.tar.gz
                """.formatted(OF_ABC, OF_NOTHING);

        assertThatThrownBy(() -> Sha256.statedFor("archive.tar.gz", checksums))
                .isInstanceOf(GradleException.class)
                .hasMessage("The checksums file states more than one SHA-256 for archive.tar.gz.");
    }
}
