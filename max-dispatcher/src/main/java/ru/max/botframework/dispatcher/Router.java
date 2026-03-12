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

    public Router update(EventHandler<Update> handler) {
        updates.register(Objects.requireNonNull(handler, "handler"));
        return this;
    }

    public EventObserver<Message> messages() {
        return messages;
    }

    public Router message(EventHandler<Message> handler) {
        messages.register(Objects.requireNonNull(handler, "handler"));
        return this;
    }

    public EventObserver<Callback> callbacks() {
        return callbacks;
    }

    public Router callback(EventHandler<Callback> handler) {
        callbacks.register(Objects.requireNonNull(handler, "handler"));
        return this;
    }

    public EventObserver<ErrorEvent> errors() {
        return errors;
    }

    public Router error(EventHandler<ErrorEvent> handler) {
        errors.register(Objects.requireNonNull(handler, "handler"));
        return this;
    }
}
