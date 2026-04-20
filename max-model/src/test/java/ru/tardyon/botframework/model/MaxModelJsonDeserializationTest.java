package ru.tardyon.botframework.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class MaxModelJsonDeserializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void shouldDeserializeUserFixture() throws IOException {
        User user = readFixture("user.json", User.class);

        assertThat(user.id().value()).isEqualTo("u-1");
        assertThat(user.username()).isEqualTo("alice");
        assertThat(user.bot()).isFalse();
    }

    @Test
    void shouldDeserializeBotInfoFixture() throws IOException {
        BotInfo botInfo = readFixture("bot-info.json", BotInfo.class);

        assertThat(botInfo.id().value()).isEqualTo("b-1");
        assertThat(botInfo.username()).isEqualTo("max_helper_bot");
    }

    @Test
    void shouldDeserializeChatFixture() throws IOException {
        Chat chat = readFixture("chat.json", Chat.class);

        assertThat(chat.id().value()).isEqualTo("c-100");
        assertThat(chat.type()).isEqualTo(ChatType.GROUP);
    }

    @Test
    void shouldDeserializeChatMemberFixture() throws IOException {
        ChatMember chatMember = readFixture("chat-member.json", ChatMember.class);

        assertThat(chatMember.status()).isEqualTo(ChatMemberStatus.MEMBER);
        assertThat(chatMember.chat().id().value()).isEqualTo("c-100");
        assertThat(chatMember.user().id().value()).isEqualTo("u-1");
    }

    @Test
    void shouldDeserializeMessageFixture() throws IOException {
        Message message = readFixture("message.json", Message.class);

        assertThat(message.messageId().value()).isEqualTo("m-100");
        assertThat(message.entities()).hasSize(1);
        assertThat(message.attachments()).hasSize(1);
        assertThat(message.entities().get(0).type()).isEqualTo(MessageEntityType.BOT_COMMAND);
        assertThat(message.attachments().get(0).type()).isEqualTo(MessageAttachmentType.DOCUMENT);
    }

    @Test
    void shouldDeserializeUpdateWithMessageFixture() throws IOException {
        Update update = readFixture("update-message.json", Update.class);

        assertThat(update.updateId().value()).isEqualTo("upd-1");
        assertThat(update.type()).isEqualTo(UpdateType.MESSAGE);
        assertThat(update.message()).isNotNull();
        assertThat(update.callback()).isNull();
    }

    @Test
    void shouldDeserializeUpdateWithCallbackFixture() throws IOException {
        Update update = readFixture("update-callback.json", Update.class);

        assertThat(update.updateId().value()).isEqualTo("upd-2");
        assertThat(update.type()).isEqualTo(UpdateType.CALLBACK);
        assertThat(update.callback()).isNotNull();
        assertThat(update.callback().callbackId().value()).isEqualTo("cb-1");
    }

    private <T> T readFixture(String fileName, Class<T> type) throws IOException {
        try (InputStream stream = Objects.requireNonNull(
                getClass().getResourceAsStream("/fixtures/" + fileName),
                "Fixture not found: " + fileName
        )) {
            return objectMapper.readValue(stream, type);
        }
    }
}
