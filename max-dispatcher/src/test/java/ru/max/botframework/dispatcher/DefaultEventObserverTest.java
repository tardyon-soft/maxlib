package ru.max.botframework.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DefaultEventObserverTest {

    @Test
    void registerStoresHandlersInRegistrationOrder() {
        DefaultEventObserver<String> observer = new DefaultEventObserver<>(ObserverType.UPDATE);
        EventHandler<String> first = event -> CompletableFuture.completedFuture(null);
        EventHandler<String> second = event -> CompletableFuture.completedFuture(null);

        observer.register(first);
        observer.register(second);

        assertEquals(2, observer.handlers().size());
        assertSame(first, observer.handlers().get(0));
        assertSame(second, observer.handlers().get(1));
    }

    @Test
    void notifyReturnsIgnoredWhenNoHandlersRegistered() {
        DefaultEventObserver<String> observer = new DefaultEventObserver<>(ObserverType.MESSAGE);

        HandlerExecutionResult result = observer.notify("event").toCompletableFuture().join();

        assertEquals(HandlerExecutionStatus.IGNORED, result.status());
        assertTrue(result.errorOpt().isEmpty());
    }

    @Test
    void notifyExecutesOnlyFirstHandlerForMvpFirstMatchBehavior() {
        DefaultEventObserver<String> observer = new DefaultEventObserver<>(ObserverType.CALLBACK);
        AtomicInteger firstCount = new AtomicInteger();
        AtomicInteger secondCount = new AtomicInteger();

        observer.register(event -> {
            firstCount.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        });
        observer.register(event -> {
            secondCount.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        });

        HandlerExecutionResult result = observer.notify("event").toCompletableFuture().join();

        assertEquals(HandlerExecutionStatus.HANDLED, result.status());
        assertEquals(1, firstCount.get());
        assertEquals(0, secondCount.get());
    }

    @Test
    void notifySkipsHandlerWhenFilterDoesNotMatchAndUsesNext() {
        DefaultEventObserver<String> observer = new DefaultEventObserver<>(ObserverType.MESSAGE);
        AtomicInteger firstCount = new AtomicInteger();
        AtomicInteger secondCount = new AtomicInteger();

        observer.register(Filter.of(event -> false), event -> {
            firstCount.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        });
        observer.register(Filter.of(event -> true), event -> {
            secondCount.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        });

        HandlerExecutionResult result = observer.notify("event").toCompletableFuture().join();

        assertEquals(HandlerExecutionStatus.HANDLED, result.status());
        assertEquals(0, firstCount.get());
        assertEquals(1, secondCount.get());
    }

    @Test
    void notifyReturnsIgnoredWhenAllFiltersDoNotMatch() {
        DefaultEventObserver<String> observer = new DefaultEventObserver<>(ObserverType.MESSAGE);
        observer.register(Filter.of(event -> false), event -> CompletableFuture.completedFuture(null));
        observer.register(Filter.of(event -> false), event -> CompletableFuture.completedFuture(null));

        HandlerExecutionResult result = observer.notify("event").toCompletableFuture().join();

        assertEquals(HandlerExecutionStatus.IGNORED, result.status());
    }

    @Test
    void notifyReturnsFailedWhenFilterExecutionFails() {
        DefaultEventObserver<String> observer = new DefaultEventObserver<>(ObserverType.MESSAGE);
        IllegalStateException failure = new IllegalStateException("filter failure");
        observer.register(event -> CompletableFuture.completedFuture(FilterResult.failed(failure)),
                event -> CompletableFuture.completedFuture(null));

        HandlerExecutionResult result = observer.notify("event").toCompletableFuture().join();

        assertEquals(HandlerExecutionStatus.FAILED, result.status());
        assertSame(failure, result.errorOpt().orElseThrow());
    }

    @Test
    void notifyReturnsFailedWhenHandlerThrows() {
        DefaultEventObserver<String> observer = new DefaultEventObserver<>(ObserverType.UPDATE);
        IllegalStateException failure = new IllegalStateException("boom");
        observer.register(event -> {
            throw failure;
        });

        HandlerExecutionResult result = observer.notify("event").toCompletableFuture().join();

        assertEquals(HandlerExecutionStatus.FAILED, result.status());
        assertSame(failure, result.errorOpt().orElseThrow());
    }

    @Test
    void notifyReturnsFailedWhenHandlerCompletesExceptionally() {
        DefaultEventObserver<String> observer = new DefaultEventObserver<>(ObserverType.ERROR);
        IllegalArgumentException failure = new IllegalArgumentException("async boom");
        observer.register(event -> CompletableFuture.failedFuture(failure));

        HandlerExecutionResult result = observer.notify("event").toCompletableFuture().join();

        assertEquals(HandlerExecutionStatus.FAILED, result.status());
        assertSame(failure, result.errorOpt().orElseThrow());
    }
}
