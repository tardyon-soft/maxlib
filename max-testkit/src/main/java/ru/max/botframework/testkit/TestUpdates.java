package ru.max.botframework.testkit;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import ru.max.botframework.model.Callback;
import ru.max.botframework.model.CallbackId;
import ru.max.botframework.model.Chat;
import ru.max.botframework.model.ChatId;
import ru.max.botframework.model.ChatType;
import ru.max.botframework.model.Message;
import ru.max.botframework.model.MessageId;
import ru.max.botframework.model.Update;
import ru.max.botframework.model.UpdateId;
import ru.max.botframework.model.UpdateType;
import ru.max.botframework.model.User;
import ru.max.botframework.model.UserId;

/**
 * Lightweight update fixtures for runtime tests.
 */
public final class TestUpdates {
    private static final Instant DEFAULT_TS = Instant.parse("2026-03-13T00:00:00Z");

    private TestUpdates() {
    }

    public static Update message(String text) {
        return message("u-test-1", "c-test-1", text);
    }

    public static Update message(String userId, String chatId, String text) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(chatId, "chatId");
        return new Update(
                new UpdateId("upd-msg-" + userId + "-" + chatId),
                UpdateType.MESSAGE,
                messagePayload(userId, chatId, text),
                null,
                null,
                DEFAULT_TS
        );
    }

    public static Update callback(String callbackData) {
        return callback("u-test-1", "c-test-1", callbackData);
    }

    public static Update callback(String userId, String chatId, String callbackData) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(chatId, "chatId");
        Message source = messagePayload(userId, chatId, "source");
        return new Update(
                new UpdateId("upd-cb-" + userId + "-" + chatId),
                UpdateType.CALLBACK,
                null,
                new Callback(
                        new CallbackId("cb-" + userId + "-" + chatId),
                        callbackData,
                        source.from(),
                        source,
                        DEFAULT_TS
                ),
                null,
                DEFAULT_TS
        );
    }

    private static Message messagePayload(String userId, String chatId, String text) {
        return new Message(
                new MessageId("msg-" + userId + "-" + chatId),
                new Chat(new ChatId(chatId), ChatType.PRIVATE, "Test Chat", null, null),
                new User(new UserId(userId), "user" + userId, "Test", "User", "Test User", false, "en"),
                text,
                DEFAULT_TS,
                null,
                List.of(),
                List.of()
        );
    }
}
