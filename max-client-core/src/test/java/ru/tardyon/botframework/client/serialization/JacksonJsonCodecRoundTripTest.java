package ru.tardyon.botframework.client.serialization;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.client.test.JsonFixtures;
import ru.tardyon.botframework.model.Update;
import ru.tardyon.botframework.model.UpdateType;
import ru.tardyon.botframework.model.UpdateId;

class JacksonJsonCodecRoundTripTest {

    private final JsonCodec jsonCodec = new JacksonJsonCodec();

    @Test
    void shouldRoundTripMaxModelDto() {
        Update source = new Update(new UpdateId("u-100"), UpdateType.MESSAGE, null, null, null, null);

        String json = jsonCodec.write(source);
        Update restored = jsonCodec.read(json, Update.class);

        assertThat(restored).isEqualTo(source);
    }

    @Test
    void shouldIgnoreUnknownPropertiesOnDeserialization() {
        String json = JsonFixtures.read("update-with-unknown.json");

        Update restored = jsonCodec.read(json, Update.class);

        assertThat(restored).isEqualTo(new Update(new UpdateId("u-200"), UpdateType.CALLBACK, null, null, null, null));
    }

    @Test
    void shouldExcludeNullFieldsAndWriteIsoDates() {
        SampleDto source = new SampleDto("id-1", null, Instant.parse("2026-01-01T00:00:00Z"));

        String json = jsonCodec.write(source);
        SampleDto restored = jsonCodec.read(json, SampleDto.class);

        assertThat(json).contains("\"id\":\"id-1\"");
        assertThat(json).contains("\"createdAt\":\"2026-01-01T00:00:00Z\"");
        assertThat(json).doesNotContain("optionalComment");
        assertThat(restored).isEqualTo(source);
    }

    private record SampleDto(String id, String optionalComment, Instant createdAt) {
    }
}
