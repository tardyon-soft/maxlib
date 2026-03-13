package ru.tardyon.botframework.message;

import java.util.Objects;
import ru.tardyon.botframework.client.MaxBotClient;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.Message;
import ru.tardyon.botframework.model.MessageId;
import ru.tardyon.botframework.model.UserId;

/**
 * High-level runtime-facing facade for message operations over {@link MaxBotClient}.
 */
public final class MessagingFacade {
    private static final MessageTarget.UserChatResolver UNSUPPORTED_USER_TARGET = userId -> {
        throw new IllegalStateException("user target requires MessageTarget.UserChatResolver");
    };

    private final MaxBotClient client;
    private final MessageTarget.UserChatResolver userChatResolver;

    public MessagingFacade(MaxBotClient client) {
        this(client, UNSUPPORTED_USER_TARGET);
    }

    public MessagingFacade(MaxBotClient client, MessageTarget.UserChatResolver userChatResolver) {
        this.client = Objects.requireNonNull(client, "client");
        this.userChatResolver = Objects.requireNonNull(userChatResolver, "userChatResolver");
    }

    public Message send(MessageTarget target, MessageBuilder builder) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(builder, "builder");
        return client.sendMessage(builder.toSendRequest(target.toChatId(userChatResolver)));
    }

    public Message send(ChatId chatId, MessageBuilder builder) {
        return send(MessageTarget.chat(Objects.requireNonNull(chatId, "chatId")), builder);
    }

    public Message send(UserId userId, MessageBuilder builder) {
        return send(MessageTarget.user(Objects.requireNonNull(userId, "userId")), builder);
    }

    public Message reply(Message sourceMessage, MessageBuilder builder) {
        Objects.requireNonNull(sourceMessage, "sourceMessage");
        Objects.requireNonNull(builder, "builder");
        return client.sendMessage(builder.toSendRequest(sourceMessage.chat().id(), sourceMessage.messageId()));
    }

    public boolean edit(Message sourceMessage, MessageBuilder builder) {
        Objects.requireNonNull(sourceMessage, "sourceMessage");
        Objects.requireNonNull(builder, "builder");
        return edit(sourceMessage.chat().id(), sourceMessage.messageId(), builder);
    }

    public boolean edit(ChatId chatId, MessageId messageId, MessageBuilder builder) {
        Objects.requireNonNull(builder, "builder");
        return client.editMessage(builder.toEditRequest(chatId, messageId));
    }

    public boolean delete(Message sourceMessage) {
        Objects.requireNonNull(sourceMessage, "sourceMessage");
        return delete(sourceMessage.messageId());
    }

    public boolean delete(MessageId messageId) {
        return client.deleteMessage(Objects.requireNonNull(messageId, "messageId"));
    }
}
