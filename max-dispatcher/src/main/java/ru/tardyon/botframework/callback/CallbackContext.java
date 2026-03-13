package ru.tardyon.botframework.callback;

import java.util.Objects;
import ru.tardyon.botframework.message.MessageBuilder;
import ru.tardyon.botframework.model.Callback;

/**
 * Callback-scoped helper object for handler ergonomics.
 */
public final class CallbackContext {
    private final CallbackFacade facade;
    private final Callback callback;

    CallbackContext(CallbackFacade facade, Callback callback) {
        this.facade = Objects.requireNonNull(facade, "facade");
        this.callback = Objects.requireNonNull(callback, "callback");
    }

    public Callback callback() {
        return callback;
    }

    public boolean answer(String text) {
        return facade.notify(callback, text);
    }

    public boolean answer(CallbackAnswerBuilder answer) {
        return facade.answer(callback, answer);
    }

    public boolean updateCurrentMessage(MessageBuilder message) {
        return facade.updateCurrentMessage(callback, message);
    }
}
