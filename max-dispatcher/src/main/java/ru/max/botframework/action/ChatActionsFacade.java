package ru.max.botframework.action;

import java.util.Objects;
import ru.max.botframework.client.MaxBotClient;
import ru.max.botframework.dispatcher.RuntimeContext;
import ru.max.botframework.model.Callback;
import ru.max.botframework.model.ChatAction;
import ru.max.botframework.model.ChatId;
import ru.max.botframework.model.Message;
import ru.max.botframework.model.Update;

/**
 * High-level runtime-facing facade for chat actions.
 */
public final class ChatActionsFacade {
    private final MaxBotClient client;

    public ChatActionsFacade(MaxBotClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    public boolean send(ChatId chatId, ChatAction action) {
        return client.sendChatAction(Objects.requireNonNull(chatId, "chatId"), Objects.requireNonNull(action, "action"));
    }

    public boolean send(Message message, ChatAction action) {
        Objects.requireNonNull(message, "message");
        return send(message.chat().id(), action);
    }

    public boolean send(Callback callback, ChatAction action) {
        Objects.requireNonNull(callback, "callback");
        if (callback.message() == null) {
            throw new IllegalStateException("callback message is required to resolve chat action target");
        }
        return send(callback.message().chat().id(), action);
    }

    public boolean send(RuntimeContext context, ChatAction action) {
        Objects.requireNonNull(context, "context");
        ChatId chatId = resolveChatId(context.update());
        return send(chatId, action);
    }

    public boolean typing(ChatId chatId) {
        return send(chatId, ChatAction.TYPING);
    }

    public boolean typing(RuntimeContext context) {
        return send(context, ChatAction.TYPING);
    }

    public boolean sendingPhoto(ChatId chatId) {
        return send(chatId, ChatAction.SENDING_PHOTO);
    }

    public boolean sendingPhoto(RuntimeContext context) {
        return send(context, ChatAction.SENDING_PHOTO);
    }

    public boolean sendingVideo(ChatId chatId) {
        return send(chatId, ChatAction.SENDING_VIDEO);
    }

    public boolean sendingAudio(ChatId chatId) {
        return send(chatId, ChatAction.SENDING_AUDIO);
    }

    public boolean sendingFile(ChatId chatId) {
        return send(chatId, ChatAction.SENDING_FILE);
    }

    private static ChatId resolveChatId(Update update) {
        Objects.requireNonNull(update, "update");
        if (update.message() != null) {
            return update.message().chat().id();
        }
        if (update.callback() != null && update.callback().message() != null) {
            return update.callback().message().chat().id();
        }
        throw new IllegalStateException("chat action target cannot be resolved from update");
    }
}
