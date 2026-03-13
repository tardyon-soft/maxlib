package ru.max.botframework.callback;

import java.util.Objects;
import ru.max.botframework.model.CallbackId;
import ru.max.botframework.model.request.AnswerCallbackRequest;

/**
 * Immutable high-level builder for callback answer payload.
 */
public final class CallbackAnswerBuilder {
    private final String text;
    private final boolean notify;
    private final int cacheSeconds;

    private CallbackAnswerBuilder(String text, boolean notify, int cacheSeconds) {
        this.text = text;
        this.notify = notify;
        this.cacheSeconds = cacheSeconds;
    }

    static CallbackAnswerBuilder empty() {
        return new CallbackAnswerBuilder(null, true, 0);
    }

    static CallbackAnswerBuilder withText(String text) {
        return empty().text(text);
    }

    public CallbackAnswerBuilder text(String value) {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        return new CallbackAnswerBuilder(value, notify, cacheSeconds);
    }

    public CallbackAnswerBuilder notify(boolean value) {
        return new CallbackAnswerBuilder(text, value, cacheSeconds);
    }

    public CallbackAnswerBuilder cacheSeconds(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("cacheSeconds must be non-negative");
        }
        return new CallbackAnswerBuilder(text, notify, value);
    }

    AnswerCallbackRequest toRequest(CallbackId callbackId) {
        return new AnswerCallbackRequest(
                Objects.requireNonNull(callbackId, "callbackId"),
                text,
                notify,
                cacheSeconds
        );
    }
}
