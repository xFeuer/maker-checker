package io.github.xfeuer.makerchecker.buildlogic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.gradle.api.InvalidUserDataException;
import org.junit.jupiter.api.Test;

class DatabaseImageTest {

    @Test
    void findsTheImageOfADatabase() {
        String composeFile = """
                services:
                  account-service:
                    build:
                      context: ../platform/account-service
                  account-db:
                    image: postgres:18.7-alpine
                """;

        assertThat(DatabaseImage.namedIn(composeFile)).isEqualTo("postgres:18.7-alpine");
    }

    @Test
    void takesTwoDatabasesThatRunTheSameImage() {
        String composeFile = """
                services:
                  account-db:
                    image: postgres:18.7-alpine
                  payment-db:
                    image: postgres:18.7-alpine
                """;

        assertThat(DatabaseImage.namedIn(composeFile)).isEqualTo("postgres:18.7-alpine");
    }

    @Test
    void readsAnImageInQuotesAndWithAComment() {
        String composeFile = """
                services:
                  account-db:
                    image: "postgres:18.7-alpine" # ADR 0016
                """;

        assertThat(DatabaseImage.namedIn(composeFile)).isEqualTo("postgres:18.7-alpine");
    }

    @Test
    void readsAFileWithTheLineEndingsOfWindows() {
        String composeFile = "services:\r\n  account-db:\r\n    image: postgres:18.7-alpine\r\n";

        assertThat(DatabaseImage.namedIn(composeFile)).isEqualTo("postgres:18.7-alpine");
    }

    @Test
    void skipsTheImagesOfOtherThingsAndALineThatIsCommentedOut() {
        String composeFile = """
                services:
                  proxy:
                    image: nginx:1.29
                  account-db:
                    # image: postgres:17.9-alpine
                    image: postgres:18.7-alpine
                """;

        assertThat(DatabaseImage.namedIn(composeFile)).isEqualTo("postgres:18.7-alpine");
    }

    @Test
    void refusesTwoDatabasesThatRunDifferentImages() {
        String composeFile = """
                services:
                  account-db:
                    image: postgres:18.7-alpine
                  payment-db:
                    image: postgres:19.0-alpine
                """;

        assertThatThrownBy(() -> DatabaseImage.namedIn(composeFile))
                .isInstanceOf(InvalidUserDataException.class)
                .hasMessageContaining("names postgres:18.7-alpine and postgres:19.0-alpine")
                .hasMessageContaining("docs/adr/0016");
    }

    @Test
    void refusesAComposeFileWithoutADatabase() {
        String composeFile = """
                services:
                  account-service:
                    build:
                      context: ../platform/account-service
                """;

        assertThatThrownBy(() -> DatabaseImage.namedIn(composeFile))
                .isInstanceOf(InvalidUserDataException.class)
                .hasMessageContaining("names none");
    }

    @Test
    void refusesAnImageWithoutATag() {
        String composeFile = """
                services:
                  account-db:
                    image: postgres
                """;

        assertThatThrownBy(() -> DatabaseImage.namedIn(composeFile)).isInstanceOf(InvalidUserDataException.class);
    }
}
