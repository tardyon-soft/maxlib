package ru.tardyon.botframework.client.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PaginationHelperTest {

    @Test
    void shouldDelegateQueryParametersMapping() {
        MarkerPageRequest request = new MarkerPageRequest(20, "mk-1");

        assertThat(PaginationHelper.toMarkerQueryParameters(request))
                .isEqualTo(Map.of("limit", "20", "marker", "mk-1"));
        assertThat(PaginationHelper.toMarkerQueryParameters(request, "page_size", "cursor"))
                .isEqualTo(Map.of("page_size", "20", "cursor", "mk-1"));
    }

    @Test
    void shouldCollectAllItemsAcrossMarkerPages() {
        MarkerPageRequest first = MarkerPageRequest.firstPage(2);

        List<String> result = PaginationHelper.collectAll(first, request -> switch (request.marker()) {
            case "" -> new MarkerPage<>(List.of("a", "b"), "m2");
            case "m2" -> new MarkerPage<>(List.of("c"), "");
            default -> throw new IllegalStateException("Unexpected marker: " + request.marker());
        });

        assertThat(result).containsExactly("a", "b", "c");
    }

    @Test
    void shouldFailWhenPageCountLimitExceeded() {
        MarkerPageRequest first = MarkerPageRequest.firstPage(1);

        assertThatThrownBy(() -> PaginationHelper.collectAll(first, request -> new MarkerPage<>(List.of("x"), "next"), 2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("maxPages=2");
    }

    @Test
    void shouldValidateMaxPagesAndFetcherResult() {
        MarkerPageRequest first = MarkerPageRequest.firstPage(1);

        assertThatThrownBy(() -> PaginationHelper.collectAll(first, req -> new MarkerPage<>(List.of(), ""), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxPages must be positive");

        AtomicInteger calls = new AtomicInteger();
        assertThatThrownBy(() -> PaginationHelper.collectAll(first, req -> {
            calls.incrementAndGet();
            return null;
        }))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("fetchPage must return non-null page");

        assertThat(calls.get()).isEqualTo(1);
    }
}
