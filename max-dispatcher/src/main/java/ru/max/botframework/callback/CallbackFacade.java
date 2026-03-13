package ru.max.botframework.callback;

import java.util.Objects;
import ru.max.botframework.client.MaxBotClient;
import ru.max.botframework.message.MessageBuilder;
import ru.max.botframework.model.Callback;

/**
 * High-level facade for callback-specific operations.
 */
public final class CallbackFacade {
    private final MaxBotClient client;

    public CallbackFacade(MaxBotClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    public CallbackContext context(Callback callback) {
        return new CallbackContext(this, callback);
    }

    public boolean notify(Callback callback, String text) {
        return answer(callback, CallbackAnswers.text(text));
    }

    public boolean answer(Callback callback, CallbackAnswerBuilder answer) {
        Objects.requireNonNull(callback, "callback");
        Objects.requireNonNull(answer, "answer");
        return client.answerCallback(answer.toRequest(callback.callbackId()));
    }

    public boolean updateCurrentMessage(Callback callback, MessageBuilder message) {
        Objects.requireNonNull(callback, "callback");
        Objects.requireNonNull(message, "message");
        if (callback.message() == null) {
            throw new IllegalStateException("callback message is required to update current message");
        }

        return client.editMessage(message.toEditRequest(
                callback.message().chat().id(),
                callback.message().messageId()
        ));
    }
}
