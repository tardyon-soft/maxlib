package ru.max.botframework.testkit;

import java.time.Instant;
import java.util.ArrayList;
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
 * Fixture builders for creating test updates with small, useful DSL.
 */
public final class UpdateFixtures {
    private static final Instant DEFAULT_TS = Instant.parse("2026-03-13T00:00:00Z");

    private UpdateFixtures() {
    }

    public static MessageUpdateBuilder message() {
        return new MessageUpdateBuilder();
    }

    public static CallbackUpdateBuilder callback() {
        return new CallbackUpdateBuilder();
    }

    /**
     * Produces sequential message updates for stateful scenarios under the same user/chat scope.
     */
    public static List<Update> statefulMessages(String userId, String chatId, String... texts) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(chatId, "chatId");
        Objects.requireNonNull(texts, "texts");

        ArrayList<Update> updates = new ArrayList<>(texts.length);
        for (int i = 0; i < texts.length; i++) {
            String text = texts[i] == null ? "" : texts[i];
            updates.add(message()
                    .userId(userId)
                    .chatId(chatId)
                    .text(text)
                    .updateId("upd-msg-%s-%s-%d".formatted(userId, chatId, i + 1))
                    .messageId("msg-%s-%s-%d".formatted(userId, chatId, i + 1))
                    .build());
        }
        return List.copyOf(updates);
    }

    public static final class MessageUpdateBuilder {
        private String updateId = "upd-msg-u-test-1-c-test-1";
        private String userId = "u-test-1";
        private String chatId = "c-test-1";
        private String text = "hello";
        private String messageId = "msg-u-test-1-c-test-1";
        private Instant eventAt = DEFAULT_TS;
        private ChatType chatType = ChatType.PRIVATE;

        private MessageUpdateBuilder() {
        }

        public MessageUpdateBuilder updateId(String updateId) {
            this.updateId = Objects.requireNonNull(updateId, "updateId");
            return this;
        }

        public MessageUpdateBuilder userId(String userId) {
            this.userId = Objects.requireNonNull(userId, "userId");
            return this;
        }

        public MessageUpdateBuilder chatId(String chatId) {
            this.chatId = Objects.requireNonNull(chatId, "chatId");
            return this;
        }

        public MessageUpdateBuilder messageId(String messageId) {
            this.messageId = Objects.requireNonNull(messageId, "messageId");
            return this;
        }

        public MessageUpdateBuilder text(String text) {
            this.text = text;
            return this;
        }

        public MessageUpdateBuilder eventAt(Instant eventAt) {
            this.eventAt = Objects.requireNonNull(eventAt, "eventAt");
            return this;
        }

        public MessageUpdateBuilder chatType(ChatType chatType) {
            this.chatType = Objects.requireNonNull(chatType, "chatType");
            return this;
        }

        public Update build() {
            Message message = new Message(
                    new MessageId(messageId),
                    new Chat(new ChatId(chatId), chatType, "Test Chat", null, null),
                    new User(new UserId(userId), "user" + userId, "Test", "User", "Test User", false, "en"),
                    text,
                    eventAt,
                    null,
                    List.of(),
                    List.of()
            );
            return new Update(new UpdateId(updateId), UpdateType.MESSAGE, message, null, null, eventAt);
        }
    }

    public static final class CallbackUpdateBuilder {
        private String updateId = "upd-cb-u-test-1-c-test-1";
        private String callbackId = "cb-u-test-1-c-test-1";
        private String userId = "u-test-1";
        private String chatId = "c-test-1";
        private String data = "action:1";
        private String sourceMessageId = "msg-src-u-test-1-c-test-1";
        private String sourceMessageText = "source";
        private Instant eventAt = DEFAULT_TS;

        private CallbackUpdateBuilder() {
        }

        public CallbackUpdateBuilder updateId(String updateId) {
            this.updateId = Objects.requireNonNull(updateId, "updateId");
            return this;
        }

        public CallbackUpdateBuilder callbackId(String callbackId) {
            this.callbackId = Objects.requireNonNull(callbackId, "callbackId");
            return this;
        }

        public CallbackUpdateBuilder userId(String userId) {
            this.userId = Objects.requireNonNull(userId, "userId");
            return this;
        }

        public CallbackUpdateBuilder chatId(String chatId) {
            this.chatId = Objects.requireNonNull(chatId, "chatId");
            return this;
        }

        public CallbackUpdateBuilder data(String data) {
            this.data = data;
            return this;
        }

        public CallbackUpdateBuilder sourceMessageId(String sourceMessageId) {
            this.sourceMessageId = Objects.requireNonNull(sourceMessageId, "sourceMessageId");
            return this;
        }

        public CallbackUpdateBuilder sourceMessageText(String sourceMessageText) {
            this.sourceMessageText = sourceMessageText;
            return this;
        }

        public CallbackUpdateBuilder eventAt(Instant eventAt) {
            this.eventAt = Objects.requireNonNull(eventAt, "eventAt");
            return this;
        }

        public Update build() {
            Message source = new Message(
                    new MessageId(sourceMessageId),
                    new Chat(new ChatId(chatId), ChatType.PRIVATE, "Test Chat", null, null),
                    new User(new UserId(userId), "user" + userId, "Test", "User", "Test User", false, "en"),
                    sourceMessageText,
                    eventAt,
                    null,
                    List.of(),
                    List.of()
            );
            Callback callback = new Callback(
                    new CallbackId(callbackId),
                    data,
                    source.from(),
                    source,
                    eventAt
            );
            return new Update(new UpdateId(updateId), UpdateType.CALLBACK, null, callback, null, eventAt);
        }
    }
}
