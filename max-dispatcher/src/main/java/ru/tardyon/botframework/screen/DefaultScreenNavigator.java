package ru.tardyon.botframework.screen;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tardyon.botframework.dispatcher.RuntimeContext;
import ru.tardyon.botframework.fsm.FSMContext;

/**
 * Default implementation of screen stack navigation.
 */
public final class DefaultScreenNavigator implements ScreenNavigator {
    private static final Logger log = LoggerFactory.getLogger(DefaultScreenNavigator.class);
    private final RuntimeContext runtime;
    private final FSMContext fsm;
    private final ScreenRegistry registry;
    private final ScreenStorage storage;
    private final ScreenRenderer renderer;
    private final Clock clock;

    public DefaultScreenNavigator(
            RuntimeContext runtime,
            FSMContext fsm,
            ScreenRegistry registry,
            ScreenStorage storage,
            ScreenRenderer renderer
    ) {
        this(runtime, fsm, registry, storage, renderer, Clock.systemUTC());
    }

    DefaultScreenNavigator(
            RuntimeContext runtime,
            FSMContext fsm,
            ScreenRegistry registry,
            ScreenStorage storage,
            ScreenRenderer renderer,
            Clock clock
    ) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.fsm = Objects.requireNonNull(fsm, "fsm");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.storage = Objects.requireNonNull(storage, "storage");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public CompletionStage<Void> start(String screenId, Map<String, Object> params) {
        return session().thenCompose(current -> {
            ScreenSession reset = new ScreenSession(
                    current.scopeId(),
                    List.of(new ScreenStackEntry(normalizeId(screenId), copyParams(params), now())),
                    current.rootMessageId(),
                    now()
            );
            return renderAndStore(reset).thenAccept(ignored -> {
            });
        });
    }

    @Override
    public CompletionStage<Void> push(String screenId, Map<String, Object> params) {
        return session().thenCompose(current -> {
            ArrayList<ScreenStackEntry> stack = new ArrayList<>(current.stack());
            stack.add(new ScreenStackEntry(normalizeId(screenId), copyParams(params), now()));
            ScreenSession next = new ScreenSession(current.scopeId(), stack, current.rootMessageId(), now());
            return renderAndStore(next).thenAccept(ignored -> {
            });
        });
    }

    @Override
    public CompletionStage<Void> replace(String screenId, Map<String, Object> params) {
        return session().thenCompose(current -> {
            ArrayList<ScreenStackEntry> stack = new ArrayList<>(current.stack());
            if (stack.isEmpty()) {
                stack.add(new ScreenStackEntry(normalizeId(screenId), copyParams(params), now()));
            } else {
                stack.set(stack.size() - 1, new ScreenStackEntry(normalizeId(screenId), copyParams(params), now()));
            }
            ScreenSession next = new ScreenSession(current.scopeId(), stack, current.rootMessageId(), now());
            return renderAndStore(next).thenAccept(ignored -> {
            });
        });
    }

    @Override
    public CompletionStage<Boolean> back() {
        return session().thenCompose(current -> {
            if (!current.canGoBack()) {
                return CompletableFuture.completedFuture(false);
            }
            ArrayList<ScreenStackEntry> stack = new ArrayList<>(current.stack());
            stack.remove(stack.size() - 1);
            ScreenSession next = new ScreenSession(current.scopeId(), stack, current.rootMessageId(), now());
            return renderAndStore(next).thenApply(ignored -> true);
        });
    }

    @Override
    public CompletionStage<Void> rerender() {
        return session().thenCompose(current -> {
            if (current.stack().isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            return renderAndStore(current).thenAccept(ignored -> {
            });
        });
    }

    @Override
    public CompletionStage<ScreenSession> session() {
        return storage.get(fsm).thenApply(existing -> existing.orElseGet(this::emptySession));
    }

    @Override
    public CompletionStage<Void> clear() {
        return storage.clear(fsm);
    }

    CompletionStage<Void> handleAction(String action, Map<String, String> args) {
        log.debug("Screen action requested: action={}, args={}", action, args);
        WidgetRuntimeSupport.ParsedWidgetAction widgetAction = WidgetRuntimeSupport.parseAction(action);
        if (widgetAction != null) {
            return handleWidgetAction(widgetAction, args);
        }
        return session().thenCompose(current -> {
            log.debug("Screen session before action: stackSize={}, rootMessageId={}", current.stack().size(), current.rootMessageId());
            return current.top()
                    .map(top -> resolve(top.screenId())
                            .onAction(screenContext(current, top.params()), action, Map.copyOf(args))
                            .thenCompose(ignored -> rerender()))
                    .orElseGet(() -> CompletableFuture.completedFuture(null));
        });
    }

    CompletionStage<Void> handleText(String text) {
        log.debug("Screen text requested: {}", text);
        return session().thenCompose(current -> current.top()
                .map(top -> resolve(top.screenId())
                        .onText(screenContext(current, top.params()), text)
                        .thenCompose(ignored -> rerender()))
                .orElseGet(() -> CompletableFuture.completedFuture(null)));
    }

    private CompletionStage<Void> handleWidgetAction(WidgetRuntimeSupport.ParsedWidgetAction widgetAction, Map<String, String> args) {
        var dispatcherOpt = runtime.dataValue(WidgetRuntimeSupport.WIDGET_ACTION_DISPATCHER_KEY);
        if (dispatcherOpt.isEmpty()) {
            log.debug("Widget action skipped: no WidgetActionDispatcher configured, widget={}, action={}",
                    widgetAction.widgetId(), widgetAction.action());
            return CompletableFuture.completedFuture(null);
        }
        return session().thenCompose(current -> {
            Map<String, Object> params = current.top().map(ScreenStackEntry::params).orElse(Map.of());
            WidgetContext context = new WidgetContext(
                    screenContext(current, params),
                    widgetAction.widgetId(),
                    params,
                    runtime.update().message(),
                    runtime.update().callback()
            );
            return dispatcherOpt.orElseThrow()
                    .dispatch(context, widgetAction.action(), Map.copyOf(args))
                    .thenCompose(effect -> effect == WidgetEffect.RERENDER ? rerender() : CompletableFuture.completedFuture(null));
        });
    }

    CompletionStage<Boolean> hasActiveScreen() {
        return session().thenApply(session -> !session.stack().isEmpty());
    }

    private CompletionStage<ScreenSession> renderAndStore(ScreenSession session) {
        if (session.stack().isEmpty()) {
            return storage.set(fsm, session).thenApply(ignored -> session);
        }
        ScreenStackEntry top = session.top().orElseThrow();
        log.debug("Rendering screen: id={}, stackSize={}", top.screenId(), session.stack().size());
        ScreenDefinition definition = resolve(top.screenId());
        ScreenContext context = screenContext(session, top.params());
        return definition.render(context)
                .thenCompose(model -> renderer.render(context, model))
                .thenCompose(result -> {
                    ScreenSession updated = new ScreenSession(
                            session.scopeId(),
                            session.stack(),
                            result.messageId(),
                            now()
                    );
                    return storage.set(fsm, updated).thenApply(ignored -> updated);
                });
    }

    private ScreenContext screenContext(ScreenSession session, Map<String, Object> params) {
        return new DefaultScreenContext(runtime, fsm, session, params, this);
    }

    private ScreenDefinition resolve(String id) {
        return registry.find(id).orElseThrow(() -> new IllegalStateException("Screen not found: " + id));
    }

    private ScreenSession emptySession() {
        return new ScreenSession(fsm.scope().toString(), List.of(), null, now());
    }

    private Instant now() {
        return clock.instant();
    }

    private static String normalizeId(String screenId) {
        Objects.requireNonNull(screenId, "screenId");
        String value = screenId.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("screenId must not be blank");
        }
        return value;
    }

    private static Map<String, Object> copyParams(Map<String, Object> params) {
        return params == null ? Map.of() : Map.copyOf(params);
    }
}
