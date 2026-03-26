package ru.tardyon.botframework.screen;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tardyon.botframework.dispatcher.DispatchResult;
import ru.tardyon.botframework.dispatcher.Filter;
import ru.tardyon.botframework.dispatcher.FilterResult;
import ru.tardyon.botframework.dispatcher.Router;
import ru.tardyon.botframework.model.Message;

/**
 * Helper that wires generic screen handlers into router.
 */
public final class ScreenRouter {
    private static final Logger log = LoggerFactory.getLogger(ScreenRouter.class);

    private ScreenRouter() {
    }

    public static Router attach(Router router, ScreenRegistry registry) {
        Objects.requireNonNull(router, "router");
        Objects.requireNonNull(registry, "registry");

        router.callback(
                callback -> CompletableFuture.completedFuture(
                        callback != null && callback.data() != null && callback.data().startsWith("ui:")
                                ? FilterResult.matched()
                                : FilterResult.notMatched()
                ),
                (callback, context) -> handleCallback(context, registry, callback.data()).thenApply(ignored -> null)
        );

        router.message(activeScreenTextFilter(registry), (message, context) ->
                handleText(context, registry, message.text()).thenApply(ignored -> null));

        return router;
    }

    public static CompletionStage<DispatchResult> middlewareHandler(
            ru.tardyon.botframework.dispatcher.RuntimeContext context,
            ScreenRegistry registry
    ) {
        if (context.update().callback() != null && context.update().callback().data() != null
                && context.update().callback().data().startsWith("ui:")) {
            return handleCallback(context, registry, context.update().callback().data())
                    .thenApply(ignored -> DispatchResult.handled());
        }
        if (context.update().message() != null && context.update().message().text() != null) {
            String text = context.update().message().text();
            if (!text.startsWith("/")) {
                ScreenNavigator navigator = Screens.navigator(context, registry);
                DefaultScreenNavigator defaultNavigator = (DefaultScreenNavigator) navigator;
                return defaultNavigator.hasActiveScreen().thenCompose(active -> {
                    if (!active) {
                        return CompletableFuture.completedFuture(DispatchResult.ignored());
                    }
                    return handleText(context, registry, text).thenApply(ignored -> DispatchResult.handled());
                });
            }
        }
        return CompletableFuture.completedFuture(DispatchResult.ignored());
    }

    private static CompletionStage<Void> handleCallback(
            ru.tardyon.botframework.dispatcher.RuntimeContext context,
            ScreenRegistry registry,
            String callbackData
    ) {
        ScreenNavigator navigator;
        try {
            navigator = Screens.navigator(context, registry);
        } catch (RuntimeException runtimeException) {
            log.debug("Screen callback skipped: navigator unavailable", runtimeException);
            return CompletableFuture.completedFuture(null);
        }
        DefaultScreenNavigator defaultNavigator = (DefaultScreenNavigator) navigator;
        var parsed = ScreenCallbackCodec.parse(callbackData);
        if (parsed.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        ScreenCallbackCodec.ParsedScreenCallback callback = parsed.orElseThrow();
        log.debug("Screen callback received: kind={}, action={}, args={}", callback.kind(), callback.action(), callback.args());
        return switch (callback.kind()) {
            case NAV_BACK -> navigator.back().thenAccept(ignored -> {
            });
            case NAV_HOME -> navigator.start("home", java.util.Map.of());
            case NAV_REFRESH -> navigator.rerender();
            case ACTION -> defaultNavigator.handleAction(callback.action(), callback.args());
            case UNKNOWN -> CompletableFuture.completedFuture(null);
        };
    }

    private static CompletionStage<Void> handleText(
            ru.tardyon.botframework.dispatcher.RuntimeContext context,
            ScreenRegistry registry,
            String text
    ) {
        ScreenNavigator navigator;
        try {
            navigator = Screens.navigator(context, registry);
        } catch (RuntimeException runtimeException) {
            log.debug("Screen text skipped: navigator unavailable", runtimeException);
            return CompletableFuture.completedFuture(null);
        }
        DefaultScreenNavigator defaultNavigator = (DefaultScreenNavigator) navigator;
        return defaultNavigator.handleText(text);
    }

    private static Filter<Message> activeScreenTextFilter(ScreenRegistry registry) {
        return new Filter<>() {
            @Override
            public CompletionStage<FilterResult> test(Message event, ru.tardyon.botframework.dispatcher.RuntimeContext context) {
                if (event == null || event.text() == null || event.text().isBlank() || event.text().startsWith("/")) {
                    return CompletableFuture.completedFuture(FilterResult.notMatched());
                }
                ScreenNavigator navigator;
                try {
                    navigator = Screens.navigator(context, registry);
                } catch (RuntimeException runtimeException) {
                    log.debug("Screen text filter skipped: navigator unavailable", runtimeException);
                    return CompletableFuture.completedFuture(FilterResult.notMatched());
                }
                DefaultScreenNavigator defaultNavigator = (DefaultScreenNavigator) navigator;
                return defaultNavigator.hasActiveScreen()
                        .thenApply(active -> active ? FilterResult.matched() : FilterResult.notMatched());
            }

            @Override
            public CompletionStage<FilterResult> test(Message event) {
                return CompletableFuture.completedFuture(FilterResult.notMatched());
            }
        };
    }
}
