package ru.tardyon.botframework.callback;

import java.util.Objects;
import ru.tardyon.botframework.client.MaxBotClient;
import ru.tardyon.botframework.message.MessageBuilder;
import ru.tardyon.botframework.model.Callback;

/**
 * High-level facade for callback-specific operations.
 */
public final class CallbackFacade {
    private final MaxBotClient client;

    public CallbackFacade(MaxBotClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    /**
     * Creates callback-scoped helper for handler ergonomics.
     */
    public CallbackContext context(Callback callback) {
        return new CallbackContext(this, callback);
    }

    /**
     * Sends lightweight notification callback answer.
     */
    public boolean notify(Callback callback, String text) {
        return answer(callback, CallbackAnswers.text(text));
    }

    /**
     * Sends callback answer built with {@link CallbackAnswerBuilder}.
     */
    public boolean answer(Callback callback, CallbackAnswerBuilder answer) {
        Objects.requireNonNull(callback, "callback");
        Objects.requireNonNull(answer, "answer");
        return client.answerCallback(answer.toRequest(callback.callbackId()));
    }

    /**
     * Edits message associated with callback event.
     */
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
