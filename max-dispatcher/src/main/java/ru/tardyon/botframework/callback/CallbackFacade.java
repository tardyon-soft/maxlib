package ru.tardyon.botframework.callback;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tardyon.botframework.client.MaxBotClient;
import ru.tardyon.botframework.client.error.MaxBadRequestException;
import ru.tardyon.botframework.message.MessageBuilder;
import ru.tardyon.botframework.model.Callback;
import ru.tardyon.botframework.model.ChatId;
import ru.tardyon.botframework.model.request.AnswerCallbackRequest;

/**
 * High-level facade for callback-specific operations.
 */
public final class CallbackFacade {
    private static final Logger log = LoggerFactory.getLogger(CallbackFacade.class);
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
        Objects.requireNonNull(callback, "callback");
        try {
            return answer(callback, CallbackAnswers.text(text));
        } catch (MaxBadRequestException badRequest) {
            // Fallback for MAX API nodes that reject notification mode and accept only message mode.
            return client.answerCallback(new AnswerCallbackRequest(callback.callbackId(), text, false, 0));
        }
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
            log.debug(
                    "Skipping callback message update: callback {} has no source message payload",
                    callback.callbackId().value()
            );
            return false;
        }
        String messageId = callback.message().messageId() == null ? null : callback.message().messageId().value();
        String chatId = callback.message().chat() == null || callback.message().chat().id() == null
                ? null
                : callback.message().chat().id().value();
        if (isUnknownId(messageId) || isUnknownId(chatId)) {
            log.debug(
                    "Callback {} has unresolved source ids (chatId={}, messageId={}); falling back to sendMessage",
                    callback.callbackId().value(),
                    chatId,
                    messageId
            );
            if (isUnknownId(chatId)) {
                return false;
            }
            client.sendMessage(message.toSendRequest(new ChatId(chatId)));
            return true;
        }

        return client.editMessage(message.toEditRequest(
                callback.message().chat().id(),
                callback.message().messageId()
        ));
    }

    private static boolean isUnknownId(String value) {
        return value == null || value.isBlank() || value.endsWith("-unknown");
    }
}
