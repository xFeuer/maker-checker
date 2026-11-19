package io.github.xfeuer.makerchecker.automation.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

class JsonTest {

    record Sample(String name) {}

    @Test
    void readsTheFieldsOfTheType() {
        Sample sample = Json.read("{\"name\": \"a\"}", Sample.class);

        assertThat(sample).isEqualTo(new Sample("a"));
    }

    @Test
    void rejectsAFieldThatTheTypeDoesNotHave() {
        assertThatExceptionOfType(UnrecognizedPropertyException.class)
                .isThrownBy(() -> Json.read("{\"name\": \"a\", \"extra\": 1}", Sample.class));
    }
}
