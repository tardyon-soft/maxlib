package ru.max.botframework.dispatcher;

import java.lang.reflect.Method;
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
    private final ArrayList<InnerMiddleware> innerMiddlewares;
    private Router parent;

    public Router(String name) {
        this.name = Objects.requireNonNull(name, "name");
        this.updates = new DefaultEventObserver<>(ObserverType.UPDATE);
        this.messages = new DefaultEventObserver<>(ObserverType.MESSAGE);
        this.callbacks = new DefaultEventObserver<>(ObserverType.CALLBACK);
        this.errors = new DefaultEventObserver<>(ObserverType.ERROR);
        this.children = new ArrayList<>();
        this.innerMiddlewares = new ArrayList<>();
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
     * Registers generic update handler with runtime context access.
     */
    public Router update(ContextualEventHandler<Update> handler) {
        updates.register(Objects.requireNonNull(handler, "handler"));
        return this;
    }

    /**
     * Registers reflective update handler resolved by dispatcher invocation engine.
     */
    public Router update(Object target, Method method) {
        updates.register(ReflectiveEventHandler.of(target, method));
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
     * Registers generic update handler with explicit filter and runtime context access.
     */
    public Router update(Filter<Update> filter, ContextualEventHandler<Update> handler) {
        updates.register(Objects.requireNonNull(filter, "filter"), Objects.requireNonNull(handler, "handler"));
        return this;
    }

    /**
     * Registers reflective update handler with explicit filter.
     */
    public Router update(Filter<Update> filter, Object target, Method method) {
        updates.register(Objects.requireNonNull(filter, "filter"), ReflectiveEventHandler.of(target, method));
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
     * Registers message handler with runtime context access.
     */
    public Router message(ContextualEventHandler<Message> handler) {
        messages.register(Objects.requireNonNull(handler, "handler"));
        return this;
    }

    /**
     * Registers reflective message handler resolved by dispatcher invocation engine.
     */
    public Router message(Object target, Method method) {
        messages.register(ReflectiveEventHandler.of(target, method));
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
     * Registers message handler with explicit filter and runtime context access.
     */
    public Router message(Filter<Message> filter, ContextualEventHandler<Message> handler) {
        messages.register(Objects.requireNonNull(filter, "filter"), Objects.requireNonNull(handler, "handler"));
        return this;
    }

    /**
     * Registers reflective message handler with explicit filter.
     */
    public Router message(Filter<Message> filter, Object target, Method method) {
        messages.register(Objects.requireNonNull(filter, "filter"), ReflectiveEventHandler.of(target, method));
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
     * Registers callback handler with runtime context access.
     */
    public Router callback(ContextualEventHandler<Callback> handler) {
        callbacks.register(Objects.requireNonNull(handler, "handler"));
        return this;
    }

    /**
     * Registers reflective callback handler resolved by dispatcher invocation engine.
     */
    public Router callback(Object target, Method method) {
        callbacks.register(ReflectiveEventHandler.of(target, method));
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
     * Registers callback handler with explicit filter and runtime context access.
     */
    public Router callback(Filter<Callback> filter, ContextualEventHandler<Callback> handler) {
        callbacks.register(Objects.requireNonNull(filter, "filter"), Objects.requireNonNull(handler, "handler"));
        return this;
    }

    /**
     * Registers reflective callback handler with explicit filter.
     */
    public Router callback(Filter<Callback> filter, Object target, Method method) {
        callbacks.register(Objects.requireNonNull(filter, "filter"), ReflectiveEventHandler.of(target, method));
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
     * Registers runtime error handler with runtime context access.
     */
    public Router error(ContextualEventHandler<ErrorEvent> handler) {
        errors.register(Objects.requireNonNull(handler, "handler"));
        return this;
    }

    /**
     * Registers reflective runtime error handler resolved by dispatcher invocation engine.
     */
    public Router error(Object target, Method method) {
        errors.register(ReflectiveEventHandler.of(target, method));
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
     * Registers runtime error handler with explicit filter and runtime context access.
     */
    public Router error(Filter<ErrorEvent> filter, ContextualEventHandler<ErrorEvent> handler) {
        errors.register(Objects.requireNonNull(filter, "filter"), Objects.requireNonNull(handler, "handler"));
        return this;
    }

    /**
     * Registers reflective runtime error handler with explicit filter.
     */
    public Router error(Filter<ErrorEvent> filter, Object target, Method method) {
        errors.register(Objects.requireNonNull(filter, "filter"), ReflectiveEventHandler.of(target, method));
        return this;
    }

    /**
     * Registers router-scoped inner middleware.
     */
    public Router innerMiddleware(InnerMiddleware middleware) {
        innerMiddlewares.add(Objects.requireNonNull(middleware, "middleware"));
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
     * Immutable snapshot of inner middleware chain in registration order.
     */
    public List<InnerMiddleware> innerMiddlewares() {
        return Collections.unmodifiableList(innerMiddlewares);
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
