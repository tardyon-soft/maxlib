package ru.max.botframework.dispatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
    private final ArrayList<Router> children;
    private Router parent;

    public Router(String name) {
        this.name = Objects.requireNonNull(name, "name");
        this.updates = new DefaultEventObserver<>(ObserverType.UPDATE);
        this.messages = new DefaultEventObserver<>(ObserverType.MESSAGE);
        this.callbacks = new DefaultEventObserver<>(ObserverType.CALLBACK);
        this.errors = new DefaultEventObserver<>(ObserverType.ERROR);
        this.children = new ArrayList<>();
    }

    public String name() {
        return name;
    }

    /**
     * Low-level access to generic update observer registry.
     */
    public EventObserver<Update> updates() {
        return updates;
    }

    /**
     * Registers generic update handler.
     */
    public Router update(EventHandler<Update> handler) {
        updates.register(Objects.requireNonNull(handler, "handler"));
        return this;
    }

    /**
     * Registers generic update handler with explicit filter.
     */
    public Router update(Filter<Update> filter, EventHandler<Update> handler) {
        updates.register(Objects.requireNonNull(filter, "filter"), Objects.requireNonNull(handler, "handler"));
        return this;
    }

    /**
     * Low-level access to message observer registry.
     */
    public EventObserver<Message> messages() {
        return messages;
    }

    /**
     * Registers message handler.
     */
    public Router message(EventHandler<Message> handler) {
        messages.register(Objects.requireNonNull(handler, "handler"));
        return this;
    }

    /**
     * Registers message handler with explicit filter.
     */
    public Router message(Filter<Message> filter, EventHandler<Message> handler) {
        messages.register(Objects.requireNonNull(filter, "filter"), Objects.requireNonNull(handler, "handler"));
        return this;
    }

    /**
     * Low-level access to callback observer registry.
     */
    public EventObserver<Callback> callbacks() {
        return callbacks;
    }

    /**
     * Registers callback handler.
     */
    public Router callback(EventHandler<Callback> handler) {
        callbacks.register(Objects.requireNonNull(handler, "handler"));
        return this;
    }

    /**
     * Registers callback handler with explicit filter.
     */
    public Router callback(Filter<Callback> filter, EventHandler<Callback> handler) {
        callbacks.register(Objects.requireNonNull(filter, "filter"), Objects.requireNonNull(handler, "handler"));
        return this;
    }

    /**
     * Low-level access to error observer registry.
     */
    public EventObserver<ErrorEvent> errors() {
        return errors;
    }

    /**
     * Registers runtime error handler.
     */
    public Router error(EventHandler<ErrorEvent> handler) {
        errors.register(Objects.requireNonNull(handler, "handler"));
        return this;
    }

    /**
     * Registers runtime error handler with explicit filter.
     */
    public Router error(Filter<ErrorEvent> filter, EventHandler<ErrorEvent> handler) {
        errors.register(Objects.requireNonNull(filter, "filter"), Objects.requireNonNull(handler, "handler"));
        return this;
    }

    /**
     * Includes child router into this router tree.
     *
     * <p>Rules:
     * 1. self-include is forbidden;
     * 2. cycles are forbidden;
     * 3. child can have only one parent.</p>
     */
    public Router includeRouter(Router child) {
        Router router = Objects.requireNonNull(child, "child");
        if (router == this) {
            throw new IllegalArgumentException("router cannot include itself");
        }
        if (isAncestorOf(router)) {
            throw new IllegalArgumentException("router inclusion would create a cycle");
        }
        if (router.parent != null) {
            throw new IllegalStateException("router is already included in another parent");
        }
        children.add(router);
        router.parent = this;
        return this;
    }

    public Optional<Router> parent() {
        return Optional.ofNullable(parent);
    }

    /**
     * Immutable snapshot of direct child routers in include order.
     */
    public List<Router> children() {
        return Collections.unmodifiableList(children);
    }

    /**
     * Returns depth-first pre-order traversal of this router subtree.
     */
    public List<Router> traversalOrder() {
        ArrayList<Router> order = new ArrayList<>();
        collectPreOrder(this, order);
        return List.copyOf(order);
    }

    private static void collectPreOrder(Router root, ArrayList<Router> order) {
        order.add(root);
        for (Router child : root.children) {
            collectPreOrder(child, order);
        }
    }

    private boolean isAncestorOf(Router candidateAncestor) {
        Router current = this;
        while (current != null) {
            if (current == candidateAncestor) {
                return true;
            }
            current = current.parent;
        }
        return false;
    }
}
