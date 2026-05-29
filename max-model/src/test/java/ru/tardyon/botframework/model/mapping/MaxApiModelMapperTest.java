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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.model.ChatMemberStatus;
import ru.tardyon.botframework.model.ChatType;
import ru.tardyon.botframework.model.MessageId;
import ru.tardyon.botframework.model.TextFormat;
import ru.tardyon.botframework.model.UpdateType;
import ru.tardyon.botframework.model.MessageAttachmentType;
import ru.tardyon.botframework.model.MessageEntityType;
import ru.tardyon.botframework.model.request.AttachmentInput;
import ru.tardyon.botframework.model.request.InlineKeyboardAttachment;
import ru.tardyon.botframework.model.request.InlineKeyboardButtonRequest;
import ru.tardyon.botframework.model.request.NewMessageBody;
import ru.tardyon.botframework.model.request.NewMessageAttachment;
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
    void mapsOutgoingReplyLinkToTopLevelMidField() throws IOException {
        ApiNewMessageBody body = MaxApiModelMapper.toApi(
                new NewMessageBody("hello", TextFormat.PLAIN, List.of()),
                true,
                new MessageId("mid.ffffbcde7cda77f3019e74224a4314c0")
        );

        String json = objectMapper.writeValueAsString(body);

        assertThat(json).contains("\"link\":{\"type\":\"reply\",\"mid\":\"mid.ffffbcde7cda77f3019e74224a4314c0\"}");
        assertThat(json).doesNotContain("\"message\":");
    }

    @Test
    void mapsIncomingApiMessageAttachments() throws IOException {
        ApiMessage api = objectMapper.readValue("""
                {
                  "sender": {
                    "user_id": 1001,
                    "first_name": "Alice",
                    "is_bot": false
                  },
                  "recipient": {
                    "chat_id": 2001,
                    "chat_type": "chat"
                  },
                  "timestamp": 1735689600,
                  "body": {
                    "mid": "3001",
                    "text": "with attachments",
                    "attachments": [
                      {
                        "type": "image",
                        "payload": {
                          "url": "https://example.com/image.png",
                          "token": "image-token-1"
                        }
                      },
                      {
                        "type": "sticker",
                        "payload": {
                          "code": "sticker-code-1"
                        }
                      },
                      {
                        "type": "location",
                        "payload": {
                          "lat": 55.7558,
                          "lon": 37.6173
                        }
                      }
                    ]
                  }
                }
                """, ApiMessage.class);

        var message = MaxApiModelMapper.toNormalized(api);

        assertThat(message.attachments()).hasSize(3);
        assertThat(message.attachments().get(0).type()).isEqualTo(MessageAttachmentType.IMAGE);
        assertThat(message.attachments().get(0).url()).isEqualTo("https://example.com/image.png");
        assertThat(message.attachments().get(1).type()).isEqualTo(MessageAttachmentType.STICKER);
        assertThat(message.attachments().get(1).payload()).isInstanceOf(Map.class);
        assertThat(message.attachments().get(2).type()).isEqualTo(MessageAttachmentType.LOCATION);
    }

    @Test
    void mapsIncomingApiMessageMarkupToEntities() throws IOException {
        ApiMessage api = objectMapper.readValue("""
                {
                  "sender": {
                    "user_id": 1001,
                    "first_name": "Alice",
                    "is_bot": false
                  },
                  "recipient": {
                    "chat_id": 2001,
                    "chat_type": "chat"
                  },
                  "timestamp": 1735689600,
                  "body": {
                    "mid": "3001",
                    "text": "bold link code",
                    "markup": [
                      {
                        "type": "strong",
                        "from": 0,
                        "length": 4
                      },
                      {
                        "type": "link",
                        "offset": 5,
                        "length": 4,
                        "url": "https://example.com"
                      },
                      {
                        "type": "monospaced",
                        "payload": {
                          "start": 10,
                          "len": 4
                        }
                      },
                      {
                        "type": "emphasized",
                        "from": 0,
                        "length": 4
                      },
                      {
                        "type": "user_mention",
                        "from": 5,
                        "length": 4,
                        "user_id": 1001
                      },
                      {
                        "type": "strikethrough",
                        "from": 0,
                        "length": 4
                      },
                      {
                        "type": "underline",
                        "from": 0,
                        "length": 4
                      },
                      {
                        "type": "heading",
                        "from": 0,
                        "length": 4
                      },
                      {
                        "type": "highlighted",
                        "from": 0,
                        "length": 4
                      },
                      {
                        "type": "quote",
                        "from": 0,
                        "length": 4
                      }
                    ]
                  }
                }
                """, ApiMessage.class);

        var message = MaxApiModelMapper.toNormalized(api);

        assertThat(message.entities()).hasSize(10);
        assertThat(message.entities().get(0).type()).isEqualTo(MessageEntityType.STRONG);
        assertThat(message.entities().get(0).offset()).isZero();
        assertThat(message.entities().get(0).length()).isEqualTo(4);
        assertThat(message.entities().get(1).type()).isEqualTo(MessageEntityType.LINK);
        assertThat(message.entities().get(1).url()).isEqualTo("https://example.com");
        assertThat(message.entities().get(1).value()).isEqualTo("https://example.com");
        assertThat(message.entities().get(2).type()).isEqualTo(MessageEntityType.MONOSPACED);
        assertThat(message.entities().get(2).offset()).isEqualTo(10);
        assertThat(message.entities().get(2).length()).isEqualTo(4);
        assertThat(message.entities().get(3).type()).isEqualTo(MessageEntityType.EMPHASIZED);
        assertThat(message.entities().get(4).type()).isEqualTo(MessageEntityType.USER_MENTION);
        assertThat(message.entities().get(4).userId()).isEqualTo(1001L);
        assertThat(message.entities().get(4).value()).isEqualTo("1001");
        assertThat(message.entities().get(5).type()).isEqualTo(MessageEntityType.STRIKETHROUGH);
        assertThat(message.entities().get(6).type()).isEqualTo(MessageEntityType.UNDERLINE);
        assertThat(message.entities().get(7).type()).isEqualTo(MessageEntityType.HEADING);
        assertThat(message.entities().get(8).type()).isEqualTo(MessageEntityType.HIGHLIGHTED);
        assertThat(message.entities().get(9).type()).isEqualTo(MessageEntityType.QUOTE);
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
    void mapsApiChatMemberUpdateToNormalizedUpdate() throws IOException {
        ApiUpdate api = objectMapper.readValue("""
                {
                  "update_type": "chat_member",
                  "timestamp": 1735689600,
                  "chat_member": {
                    "user_id": 1001,
                    "chat_id": 2001,
                    "status": "admin",
                    "user": {
                      "user_id": 1001,
                      "first_name": "Helper",
                      "username": "helper_bot",
                      "is_bot": true
                    }
                  }
                }
                """, ApiUpdate.class);

        var update = MaxApiModelMapper.toNormalized(api);

        assertThat(update.type()).isEqualTo(UpdateType.CHAT_MEMBER);
        assertThat(update.chatMember()).isNotNull();
        assertThat(update.chatMember().status()).isEqualTo(ChatMemberStatus.ADMINISTRATOR);
        assertThat(update.chatMember().chat().id().value()).isEqualTo("2001");
        assertThat(update.chatMember().user().id().value()).isEqualTo("1001");
        assertThat(update.chatMember().user().bot()).isTrue();
    }

    @Test
    void mapsApiBotAddedUpdateToNormalizedUpdate() throws IOException {
        ApiUpdate api = objectMapper.readValue("""
                {
                  "update_type": "bot_added",
                  "timestamp": 1735689600,
                  "chat_id": 2001,
                  "user": {
                    "user_id": 1001,
                    "first_name": "Alice",
                    "username": "alice",
                    "is_bot": false
                  },
                  "is_channel": true
                }
                """, ApiUpdate.class);

        var update = MaxApiModelMapper.toNormalized(api);

        assertThat(update.updateId().value()).isEqualTo("upd-bot_added-2001-1735689600");
        assertThat(update.type()).isEqualTo(UpdateType.BOT_ADDED);
        assertThat(update.chatId()).isNotNull();
        assertThat(update.chatId().value()).isEqualTo("2001");
        assertThat(update.user()).isNotNull();
        assertThat(update.user().id().value()).isEqualTo("1001");
        assertThat(update.channel()).isTrue();
    }

    @Test
    void mapsBotStartedUpdateWithPayload() throws IOException {
        ApiUpdate api = objectMapper.readValue("""
                {
                  "update_type": "bot_started",
                  "timestamp": 1573226679188,
                  "chat_id": 1234567890,
                  "payload": "promo_summer2025",
                  "user_locale": "ru-RU",
                  "user": {
                    "user_id": 1234567890,
                    "first_name": "Ivan",
                    "username": "ivan_petrov",
                    "is_bot": false
                  }
                }
                """, ApiUpdate.class);

        var update = MaxApiModelMapper.toNormalized(api);

        assertThat(update.type()).isEqualTo(UpdateType.BOT_STARTED);
        assertThat(update.rawUpdateType()).isEqualTo("bot_started");
        assertThat(update.chatId().value()).isEqualTo("1234567890");
        assertThat(update.payload()).isEqualTo("promo_summer2025");
        assertThat(update.userLocale()).isEqualTo("ru-RU");
        assertThat(update.user().id().value()).isEqualTo("1234567890");
    }

    @Test
    void mapsLifecycleUpdateTypes() throws IOException {
        assertThatUpdateType("message_edited").isEqualTo(UpdateType.MESSAGE_EDITED);
        assertThatUpdateType("message_removed").isEqualTo(UpdateType.MESSAGE_REMOVED);
        assertThatUpdateType("bot_stopped").isEqualTo(UpdateType.BOT_STOPPED);
        assertThatUpdateType("user_added").isEqualTo(UpdateType.USER_ADDED);
        assertThatUpdateType("user_removed").isEqualTo(UpdateType.USER_REMOVED);
        assertThatUpdateType("message_chat_created").isEqualTo(UpdateType.MESSAGE_CHAT_CREATED);
        assertThatUpdateType("message_construction_request").isEqualTo(UpdateType.MESSAGE_CONSTRUCTION_REQUEST);
        assertThatUpdateType("message_constructed").isEqualTo(UpdateType.MESSAGE_CONSTRUCTED);
        assertThatUpdateType("dialog_muted").isEqualTo(UpdateType.DIALOG_MUTED);
        assertThatUpdateType("dialog_unmuted").isEqualTo(UpdateType.DIALOG_UNMUTED);
        assertThatUpdateType("dialog_cleared").isEqualTo(UpdateType.DIALOG_CLEARED);
        assertThatUpdateType("dialog_removed").isEqualTo(UpdateType.DIALOG_REMOVED);
    }

    @Test
    void mapsChatTitleChangedUpdate() throws IOException {
        ApiUpdate api = objectMapper.readValue("""
                {
                  "update_type": "chat_title_changed",
                  "timestamp": 1735689600,
                  "chat_id": 2001,
                  "title": "New title",
                  "user": {
                    "user_id": 1001,
                    "first_name": "Alice",
                    "is_bot": false
                  }
                }
                """, ApiUpdate.class);

        var update = MaxApiModelMapper.toNormalized(api);

        assertThat(update.type()).isEqualTo(UpdateType.CHAT_TITLE_CHANGED);
        assertThat(update.title()).isEqualTo("New title");
        assertThat(update.chatId().value()).isEqualTo("2001");
        assertThat(update.user().id().value()).isEqualTo("1001");
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
    void mapsClipboardInlineButtonToApiPayload() throws IOException {
        NewMessageBody normalized = new NewMessageBody(
                "Copy demo",
                TextFormat.MARKDOWN,
                List.of(NewMessageAttachment.inlineKeyboard(new InlineKeyboardAttachment(List.of(
                        List.of(new InlineKeyboardButtonRequest("Copy", null, "PROMO-2026", null, null, null, null, null))
                ))))
        );

        var mapped = MaxApiModelMapper.toApiOutgoing(normalized, true, null);
        String json = objectMapper.writeValueAsString(mapped);

        assertThat(json).contains("\"type\":\"clipboard\"");
        assertThat(json).contains("\"payload\":\"PROMO-2026\"");
        assertThat(json).contains("\"type\":\"inline_keyboard\"");
    }

    @Test
    void mapsImageUrlAttachmentToDocsPayload() throws IOException {
        NewMessageBody normalized = new NewMessageBody(
                "Image demo",
                TextFormat.PLAIN,
                List.of(NewMessageAttachment.imageUrl("https://example.com/image.png"))
        );

        var mapped = MaxApiModelMapper.toApiOutgoing(normalized, true, null);
        String json = objectMapper.writeValueAsString(mapped);

        assertThat(json).contains("\"type\":\"image\"");
        assertThat(json).contains("\"payload\":{\"url\":\"https://example.com/image.png\"}");
        assertThat(json).doesNotContain("\"input\"");
    }

    @Test
    void mapsLegacyPhotoAttachmentToImagePayload() throws IOException {
        NewMessageBody normalized = new NewMessageBody(
                "Image demo",
                TextFormat.PLAIN,
                List.of(NewMessageAttachment.media(
                        MessageAttachmentType.PHOTO,
                        new AttachmentInput(null, "image-token-1", null),
                        null,
                        null,
                        null
                ))
        );

        var mapped = MaxApiModelMapper.toApiOutgoing(normalized, true, null);
        String json = objectMapper.writeValueAsString(mapped);

        assertThat(json).contains("\"type\":\"image\"");
        assertThat(json).contains("\"payload\":{\"token\":\"image-token-1\"}");
    }

    @Test
    void mapsSpecialAttachmentsToDocsPayload() throws IOException {
        NewMessageBody normalized = new NewMessageBody(
                "Special demo",
                TextFormat.PLAIN,
                List.of(
                        NewMessageAttachment.sticker("sticker-code-1"),
                        NewMessageAttachment.location(55.7558, 37.6173),
                        NewMessageAttachment.share("https://max.ru/post/1", "share-token-1")
                )
        );

        var mapped = MaxApiModelMapper.toApiOutgoing(normalized, true, null);
        String json = objectMapper.writeValueAsString(mapped);

        assertThat(json).contains("\"type\":\"sticker\"");
        assertThat(json).contains("\"payload\":{\"code\":\"sticker-code-1\"}");
        assertThat(json).contains("\"type\":\"location\"");
        assertThat(json).contains("\"payload\":{\"lat\":55.7558,\"lon\":37.6173}");
        assertThat(json).contains("\"type\":\"share\"");
        assertThat(json).contains("\"payload\":{\"url\":\"https://max.ru/post/1\",\"token\":\"share-token-1\"}");
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

    private org.assertj.core.api.AbstractObjectAssert<?, UpdateType> assertThatUpdateType(String rawType) throws IOException {
        ApiUpdate api = objectMapper.readValue("""
                {
                  "update_type": "%s",
                  "timestamp": 1735689600
                }
                """.formatted(rawType), ApiUpdate.class);
        return assertThat(MaxApiModelMapper.toNormalized(api).type());
    }
}
