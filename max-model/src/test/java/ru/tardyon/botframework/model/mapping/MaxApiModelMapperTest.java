package ru.tardyon.botframework.model.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.model.ChatType;
import ru.tardyon.botframework.model.TextFormat;
import ru.tardyon.botframework.model.UpdateType;
import ru.tardyon.botframework.model.request.NewMessageBody;
import ru.tardyon.botframework.model.transport.ApiChat;
import ru.tardyon.botframework.model.transport.ApiMessage;
import ru.tardyon.botframework.model.transport.ApiNewMessageBody;
import ru.tardyon.botframework.model.transport.ApiUpdate;
import ru.tardyon.botframework.model.transport.ApiUser;

class MaxApiModelMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void mapsApiUserToNormalizedUser() throws IOException {
        ApiUser api = readFixture("user.json", ApiUser.class);

        var user = MaxApiModelMapper.toNormalized(api);

        assertThat(user.id().value()).isEqualTo("1001");
        assertThat(user.username()).isEqualTo("alice");
        assertThat(user.firstName()).isEqualTo("Alice");
        assertThat(user.bot()).isFalse();
        assertThat(user.displayName()).isEqualTo("Alice S.");
    }

    @Test
    void mapsApiChatToNormalizedChat() throws IOException {
        ApiChat api = readFixture("chat.json", ApiChat.class);

        var chat = MaxApiModelMapper.toNormalized(api);

        assertThat(chat.id().value()).isEqualTo("2001");
        assertThat(chat.type()).isEqualTo(ChatType.GROUP);
        assertThat(chat.title()).isEqualTo("Payments");
    }

    @Test
    void mapsApiMessageToNormalizedMessage() throws IOException {
        ApiMessage api = readFixture("message.json", ApiMessage.class);

        var message = MaxApiModelMapper.toNormalized(api);

        assertThat(message.messageId().value()).isEqualTo("3001");
        assertThat(message.chat().id().value()).isEqualTo("2001");
        assertThat(message.chat().type()).isEqualTo(ChatType.GROUP);
        assertThat(message.text()).isEqualTo("hello");
        assertThat(message.createdAt()).isEqualTo(Instant.ofEpochSecond(1735689600L));
    }

    @Test
    void mapsReplyToFromLinkMessageMid() throws IOException {
        ApiMessage api = objectMapper.readValue("""
                {
                  "sender": {
                    "user_id": 1001,
                    "first_name": "Alice",
                    "username": "alice",
                    "is_bot": false
                  },
                  "recipient": {
                    "chat_id": 2001,
                    "chat_type": "chat"
                  },
                  "timestamp": 1735689600,
                  "link": {
                    "type": "forward",
                    "chat_id": 2001,
                    "message": {
                      "mid": "3000",
                      "seq": 1,
                      "text": "source"
                    }
                  },
                  "body": {
                    "mid": "3001",
                    "text": "hello"
                  }
                }
                """, ApiMessage.class);

        var message = MaxApiModelMapper.toNormalized(api);

        assertThat(message.messageId().value()).isEqualTo("3001");
        assertThat(message.replyToMessageId()).isNotNull();
        assertThat(message.replyToMessageId().value()).isEqualTo("3000");
    }

    @Test
    void mapsApiUpdateToNormalizedUpdate() throws IOException {
        ApiUpdate api = readFixture("update-message.json", ApiUpdate.class);

        var update = MaxApiModelMapper.toNormalized(api);

        assertThat(update.updateId().value()).isEqualTo("upd-msg-3001");
        assertThat(update.type()).isEqualTo(UpdateType.MESSAGE);
        assertThat(update.message()).isNotNull();
        assertThat(update.message().messageId().value()).isEqualTo("3001");
        assertThat(update.eventAt()).isEqualTo(Instant.ofEpochSecond(1735689600L));
    }

    @Test
    void mapsApiNewMessageBodyToNormalizedAndBack() throws IOException {
        ApiNewMessageBody api = readFixture("new-message-body.json", ApiNewMessageBody.class);

        NewMessageBody normalized = MaxApiModelMapper.toNormalized(api);
        ApiNewMessageBody mappedBack = MaxApiModelMapper.toApi(normalized);

        assertThat(normalized.text()).isEqualTo("Hello, MAX");
        assertThat(normalized.format()).isEqualTo(TextFormat.MARKDOWN);
        assertThat(mappedBack.text()).isEqualTo("Hello, MAX");
        assertThat(mappedBack.format()).isEqualTo(TextFormat.MARKDOWN);
        assertThat(mappedBack.notifyValue()).isTrue();
    }

    @Test
    void mapsCallbackUsingPayloadAndUserAliases() throws IOException {
        ApiUpdate api = objectMapper.readValue("""
                {
                  "update_type": "message_callback",
                  "timestamp": 1735689600,
                  "callback": {
                    "callback_id": "cb-token-1",
                    "payload": "menu:pay",
                    "user": {
                      "user_id": 1001,
                      "first_name": "Alice",
                      "username": "alice",
                      "is_bot": false
                    }
                  }
                }
                """, ApiUpdate.class);

        var update = MaxApiModelMapper.toNormalized(api);

        assertThat(update.type()).isEqualTo(UpdateType.CALLBACK);
        assertThat(update.callback()).isNotNull();
        assertThat(update.callback().callbackId().value()).isEqualTo("cb-token-1");
        assertThat(update.callback().data()).isEqualTo("menu:pay");
        assertThat(update.callback().from()).isNotNull();
        assertThat(update.callback().from().id().value()).isEqualTo("1001");
    }

    @Test
    void mapsCallbackMessageFromUpdateMessageWhenCallbackMessageMissing() throws IOException {
        ApiUpdate api = objectMapper.readValue("""
                {
                  "update_type": "message_callback",
                  "timestamp": 1735689600,
                  "message": {
                    "timestamp": 1735689600,
                    "body": {"mid": "9001", "text": "Menu"},
                    "recipient": {"chat_id": 247923392, "chat_type": "dialog"},
                    "sender": {"user_id": 1001, "first_name": "Alice", "is_bot": false}
                  },
                  "callback": {
                    "callback_id": "cb-token-2",
                    "payload": "menu:pay",
                    "user": {
                      "user_id": 1001,
                      "first_name": "Alice",
                      "username": "alice",
                      "is_bot": false
                    }
                  }
                }
                """, ApiUpdate.class);

        var update = MaxApiModelMapper.toNormalized(api);

        assertThat(update.type()).isEqualTo(UpdateType.CALLBACK);
        assertThat(update.callback()).isNotNull();
        assertThat(update.callback().message()).isNotNull();
        assertThat(update.callback().message().messageId().value()).isEqualTo("9001");
        assertThat(update.callback().message().chat().id().value()).isEqualTo("247923392");
    }

    @Test
    void mapsCallbackMessageFromChatIdAndMessageIdFallbackShape() throws IOException {
        ApiUpdate api = objectMapper.readValue("""
                {
                  "update_type": "message_callback",
                  "timestamp": 1735689600,
                  "callback": {
                    "callback_id": "cb-token-3",
                    "callback_data": "menu:help",
                    "from": {
                      "user_id": 1001,
                      "first_name": "Alice",
                      "is_bot": false
                    },
                    "chat_id": "247923392",
                    "message_id": "9010"
                  }
                }
                """, ApiUpdate.class);

        var update = MaxApiModelMapper.toNormalized(api);

        assertThat(update.callback()).isNotNull();
        assertThat(update.callback().data()).isEqualTo("menu:help");
        assertThat(update.callback().from()).isNotNull();
        assertThat(update.callback().message()).isNotNull();
        assertThat(update.callback().message().chat().id().value()).isEqualTo("247923392");
        assertThat(update.callback().message().messageId().value()).isEqualTo("9010");
    }

    private <T> T readFixture(String fileName, Class<T> type) throws IOException {
        try (InputStream stream = Objects.requireNonNull(
                getClass().getResourceAsStream("/fixtures/api-docs/" + fileName),
                "Fixture not found: " + fileName
        )) {
            return objectMapper.readValue(stream, type);
        }
    }
}
