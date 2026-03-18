package ru.tardyon.botframework.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import ru.tardyon.botframework.dispatcher.annotation.Callback;
import ru.tardyon.botframework.dispatcher.annotation.CallbackPrefix;
import ru.tardyon.botframework.dispatcher.annotation.Command;
import ru.tardyon.botframework.dispatcher.annotation.Route;
import ru.tardyon.botframework.dispatcher.annotation.Text;
import ru.tardyon.botframework.dispatcher.annotation.UseFilters;
import ru.tardyon.botframework.dispatcher.annotation.UseMiddleware;
import ru.tardyon.botframework.testkit.TestUpdates;

class AnnotatedRouteRegistrarTest {

    @Test
    void registrarMapsCommandAndCallbackAnnotationsToRouter() {
        DemoRoute route = new DemoRoute();
        Router router = new AnnotatedRouteRegistrar().register(route);
        Dispatcher dispatcher = new Dispatcher().includeRouter(router);

        DispatchResult startResult = dispatcher.feedUpdate(TestUpdates.message("/start"))
                .toCompletableFuture()
                .join();
        DispatchResult callbackResult = dispatcher.feedUpdate(TestUpdates.callback("menu:pay:42"))
                .toCompletableFuture()
                .join();

        assertEquals(DispatchStatus.HANDLED, startResult.status());
        assertEquals(DispatchStatus.HANDLED, callbackResult.status());
        assertEquals(1, route.startCalls.get());
        assertEquals(1, route.callbackCalls.get());
    }

    @Test
    void registrarAppliesUseFiltersAndUseMiddleware() {
        FilteredRoute route = new FilteredRoute();
        Router router = new AnnotatedRouteRegistrar().register(route);
        Dispatcher dispatcher = new Dispatcher().includeRouter(router);

        DispatchResult ignored = dispatcher.feedUpdate(TestUpdates.message("hello"))
                .toCompletableFuture()
                .join();
        DispatchResult handled = dispatcher.feedUpdate(TestUpdates.message("go now"))
                .toCompletableFuture()
                .join();

        assertEquals(DispatchStatus.IGNORED, ignored.status());
        assertEquals(DispatchStatus.HANDLED, handled.status());
        assertEquals(1, route.calls.get());
        assertEquals(11, route.lastAttempt);
    }

    @Test
    void exactCallbackMappingHasPriorityOverPrefixMapping() {
        CallbackPriorityRoute route = new CallbackPriorityRoute();
        Router router = new AnnotatedRouteRegistrar().register(route);
        Dispatcher dispatcher = new Dispatcher().includeRouter(router);

        DispatchResult callbackResult = dispatcher.feedUpdate(TestUpdates.callback("amenu:pay"))
                .toCompletableFuture()
                .join();

        assertEquals(DispatchStatus.HANDLED, callbackResult.status());
        assertEquals(1, route.exactCalls.get());
        assertEquals(0, route.prefixCalls.get());
    }

    @Route("menu")
    static final class DemoRoute {
        final AtomicInteger startCalls = new AtomicInteger();
        final AtomicInteger callbackCalls = new AtomicInteger();

        @Command("start")
        public CompletableFuture<Void> start() {
            startCalls.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        }

        @CallbackPrefix("menu:")
        public CompletableFuture<Void> onMenu(ru.tardyon.botframework.model.Callback callback) {
            callbackCalls.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        }
    }

    @Route("filtered")
    @UseFilters(PrefixGoFilter.class)
    static final class FilteredRoute {
        final AtomicInteger calls = new AtomicInteger();
        volatile int lastAttempt;

        @Text("go now")
        @UseMiddleware(AttemptMiddleware.class)
        public CompletableFuture<Void> onGo(Integer attempt) {
            calls.incrementAndGet();
            lastAttempt = attempt;
            return CompletableFuture.completedFuture(null);
        }
    }

    static final class PrefixGoFilter implements Filter<ru.tardyon.botframework.model.Message> {
        @Override
        public java.util.concurrent.CompletionStage<FilterResult> test(ru.tardyon.botframework.model.Message event) {
            if (event == null || event.text() == null || !event.text().startsWith("go")) {
                return CompletableFuture.completedFuture(FilterResult.notMatched());
            }
            return CompletableFuture.completedFuture(FilterResult.matched());
        }
    }

    static final class AttemptMiddleware implements InnerMiddleware {
        @Override
        public java.util.concurrent.CompletionStage<DispatchResult> invoke(RuntimeContext context, MiddlewareNext next) {
            context.putEnrichment("attempt", 11);
            return next.proceed();
        }
    }

    @Route("callback-priority")
    static final class CallbackPriorityRoute {
        final AtomicInteger exactCalls = new AtomicInteger();
        final AtomicInteger prefixCalls = new AtomicInteger();

        @CallbackPrefix("amenu:")
        public CompletableFuture<Void> anyAmenu() {
            prefixCalls.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        }

        @Callback("amenu:pay")
        public CompletableFuture<Void> payOnly() {
            exactCalls.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        }
    }
}
