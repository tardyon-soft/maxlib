package ru.tardyon.botframework.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.junit.jupiter.api.Test;

class TypedValuesSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void shouldSerializeValueObjectAsString() throws Exception {
        ChatId chatId = new ChatId("chat-42");

        String json = objectMapper.writeValueAsString(chatId);
        ChatId restored = objectMapper.readValue(json, ChatId.class);

        assertThat(json).isEqualTo("\"chat-42\"");
        assertThat(restored).isEqualTo(chatId);
    }

    @Test
    void shouldRejectBlankValueObjectValues() {
        assertThatThrownBy(() -> new MessageId(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("value must not be blank");
    }

    @Test
    void shouldSerializeAndDeserializeChatAction() throws Exception {
        String json = objectMapper.writeValueAsString(ChatAction.RECORDING_VOICE);
        ChatAction restored = objectMapper.readValue("\"recording_voice\"", ChatAction.class);

        assertThat(json).isEqualTo("\"recording_voice\"");
        assertThat(restored).isEqualTo(ChatAction.RECORDING_VOICE);
    }

    @Test
    void shouldMapUnknownAdminPermissionToUnknown() throws Exception {
        ChatAdminPermission restored = objectMapper.readValue("\"some_future_permission\"", ChatAdminPermission.class);

        assertThat(restored).isEqualTo(ChatAdminPermission.UNKNOWN);
    }
}
