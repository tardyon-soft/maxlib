package ru.max.botframework.client.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JsonFixturesTest {

    @Test
    void shouldReadFixtureContent() {
        String json = JsonFixtures.read("operation-success-response.json");

        assertThat(json).contains("\"success\": true");
    }

    @Test
    void shouldFailForUnknownFixture() {
        assertThatThrownBy(() -> JsonFixtures.read("missing.json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Fixture not found");
    }
}
