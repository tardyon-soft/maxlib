package ru.tardyon.botframework.client.serialization;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.client.test.JsonFixtures;
import ru.tardyon.botframework.model.Update;
import ru.tardyon.botframework.model.UpdateId;
import ru.tardyon.botframework.model.UpdateType;
import ru.tardyon.botframework.model.response.MessageResponse;

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

    @Test
    void shouldDeserializeMessageResponseFromRawTransportShape() {
        String json = """
                {
                  "mid": "m-raw-1",
                  "timestamp": 1735689600,
                  "recipient": {"chat_id": 247923392, "chat_type": "dialog"},
                  "sender": {"user_id": 1001, "first_name": "Alice", "is_bot": false},
                  "body": {"text": "hello raw"}
                }
                """;

        MessageResponse response = jsonCodec.read(json, MessageResponse.class);

        assertThat(response.message().messageId().value()).isEqualTo("m-raw-1");
        assertThat(response.message().chat().id().value()).isEqualTo("247923392");
        assertThat(response.message().text()).isEqualTo("hello raw");
    }

    private record SampleDto(String id, String optionalComment, Instant createdAt) {
    }
}
