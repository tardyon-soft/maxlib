package ru.max.botframework.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void andCompositionMatchesWhenBothMatched() {
        Filter<String> left = event -> java.util.concurrent.CompletableFuture.completedFuture(
                FilterResult.matched(java.util.Map.of("left", "1"))
        );
        Filter<String> right = event -> java.util.concurrent.CompletableFuture.completedFuture(
                FilterResult.matched(java.util.Map.of("right", "2"))
        );

        FilterResult result = left.and(right).test("x").toCompletableFuture().join();

        assertEquals(FilterStatus.MATCHED, result.status());
        assertEquals("1", result.enrichment().get("left"));
        assertEquals("2", result.enrichment().get("right"));
    }

    @Test
    void andCompositionReturnsNotMatchedWhenLeftNotMatched() {
        Filter<String> left = event -> java.util.concurrent.CompletableFuture.completedFuture(FilterResult.notMatched());
        Filter<String> right = event -> java.util.concurrent.CompletableFuture.completedFuture(FilterResult.matched());

        FilterResult result = left.and(right).test("x").toCompletableFuture().join();

        assertEquals(FilterStatus.NOT_MATCHED, result.status());
    }

    @Test
    void orCompositionUsesRightWhenLeftNotMatched() {
        Filter<String> left = event -> java.util.concurrent.CompletableFuture.completedFuture(FilterResult.notMatched());
        Filter<String> right = event -> java.util.concurrent.CompletableFuture.completedFuture(
                FilterResult.matched(java.util.Map.of("source", "right"))
        );

        FilterResult result = left.or(right).test("x").toCompletableFuture().join();

        assertEquals(FilterStatus.MATCHED, result.status());
        assertEquals("right", result.enrichment().get("source"));
    }

    @Test
    void notCompositionInvertsMatchWithoutEnrichmentLeak() {
        Filter<String> source = event -> java.util.concurrent.CompletableFuture.completedFuture(
                FilterResult.matched(java.util.Map.of("k", "v"))
        );

        FilterResult result = source.not().test("x").toCompletableFuture().join();

        assertEquals(FilterStatus.NOT_MATCHED, result.status());
        assertTrue(result.enrichment().isEmpty());
    }

    @Test
    void enrichmentMergeConflictReturnsNotMatched() {
        Filter<String> left = event -> java.util.concurrent.CompletableFuture.completedFuture(
                FilterResult.matched(java.util.Map.of("id", "1"))
        );
        Filter<String> right = event -> java.util.concurrent.CompletableFuture.completedFuture(
                FilterResult.matched(java.util.Map.of("id", "2"))
        );

        FilterResult result = left.and(right).test("x").toCompletableFuture().join();

        assertEquals(FilterStatus.NOT_MATCHED, result.status());
    }
}
