package io.github.xfeuer.makerchecker.automation.api;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

final class Json {

    // Strict on purpose: a field that the contract does not declare must fail the test.
    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private Json() {}

    static <T> T read(String json, Class<T> type) {
        return MAPPER.readValue(json, type);
    }
}
