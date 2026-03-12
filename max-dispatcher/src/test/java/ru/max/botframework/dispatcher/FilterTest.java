package ru.max.botframework.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class FilterTest {

    @Test
    void predicateFilterMatches() {
        Filter<String> filter = Filter.of(value -> value.startsWith("ok"));

        FilterResult result = filter.test("ok-1").toCompletableFuture().join();

        assertEquals(FilterStatus.MATCHED, result.status());
    }

    @Test
    void predicateFilterDoesNotMatch() {
        Filter<String> filter = Filter.of(value -> value.startsWith("ok"));

        FilterResult result = filter.test("nope").toCompletableFuture().join();

        assertEquals(FilterStatus.NOT_MATCHED, result.status());
    }

    @Test
    void predicateFilterReturnsFailedWhenPredicateThrows() {
        IllegalStateException failure = new IllegalStateException("boom");
        Filter<String> filter = Filter.of(value -> {
            throw failure;
        });

        FilterResult result = filter.test("x").toCompletableFuture().join();

        assertEquals(FilterStatus.FAILED, result.status());
        assertSame(failure, result.errorOpt().orElseThrow());
    }

    @Test
    void anyFilterAlwaysMatches() {
        Filter<String> filter = Filter.any();

        FilterResult result = filter.test("anything").toCompletableFuture().join();

        assertEquals(FilterStatus.MATCHED, result.status());
    }
}

