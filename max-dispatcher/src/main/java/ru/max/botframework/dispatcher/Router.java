package ru.max.botframework.dispatcher;

import java.util.Objects;
import ru.max.botframework.model.Callback;
import ru.max.botframework.model.Message;
import ru.max.botframework.model.Update;

/**
 * Logical routing unit that groups handlers by domain.
 */
public final class Router {
    private final String name;
    private final EventObserver<Update> updates;
    private final EventObserver<Message> messages;
    private final EventObserver<Callback> callbacks;
    private final EventObserver<ErrorEvent> errors;

    public Router(String name) {
        this.name = Objects.requireNonNull(name, "name");
        this.updates = new DefaultEventObserver<>(ObserverType.UPDATE);
        this.messages = new DefaultEventObserver<>(ObserverType.MESSAGE);
        this.callbacks = new DefaultEventObserver<>(ObserverType.CALLBACK);
        this.errors = new DefaultEventObserver<>(ObserverType.ERROR);
    }

    public String name() {
        return name;
    }

    public EventObserver<Update> updates() {
        return updates;
    }

    public EventObserver<Message> messages() {
        return messages;
    }

    public EventObserver<Callback> callbacks() {
        return callbacks;
    }

    public EventObserver<ErrorEvent> errors() {
        return errors;
    }
}
