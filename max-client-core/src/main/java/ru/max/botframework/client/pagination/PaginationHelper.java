package ru.max.botframework.client.pagination;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Reusable helper utilities for marker-based pagination.
 */
public final class PaginationHelper {
    private static final int DEFAULT_MAX_PAGES = 10_000;

    private PaginationHelper() {
    }

    public static Map<String, String> toMarkerQueryParameters(MarkerPageRequest request) {
        Objects.requireNonNull(request, "request");
        return request.toQueryParameters();
    }

    public static Map<String, String> toMarkerQueryParameters(
            MarkerPageRequest request,
            String limitParamName,
            String markerParamName
    ) {
        Objects.requireNonNull(request, "request");
        return request.toQueryParameters(limitParamName, markerParamName);
    }

    public static <T> List<T> collectAll(
            MarkerPageRequest firstRequest,
            Function<MarkerPageRequest, MarkerPage<T>> fetchPage
    ) {
        return collectAll(firstRequest, fetchPage, DEFAULT_MAX_PAGES);
    }

    public static <T> List<T> collectAll(
            MarkerPageRequest firstRequest,
            Function<MarkerPageRequest, MarkerPage<T>> fetchPage,
            int maxPages
    ) {
        Objects.requireNonNull(firstRequest, "firstRequest");
        Objects.requireNonNull(fetchPage, "fetchPage");
        if (maxPages <= 0) {
            throw new IllegalArgumentException("maxPages must be positive");
        }

        List<T> collected = new ArrayList<>();
        MarkerPageRequest request = firstRequest;
        int pagesFetched = 0;

        while (pagesFetched < maxPages) {
            MarkerPage<T> page = Objects.requireNonNull(fetchPage.apply(request), "fetchPage must return non-null page");
            collected.addAll(page.items());
            pagesFetched++;

            if (!page.hasNext()) {
                return List.copyOf(collected);
            }

            request = page.nextPageRequest(request)
                    .orElseThrow(() -> new IllegalStateException("Page has next marker but next request cannot be created"));
        }

        throw new IllegalStateException("Pagination exceeded maxPages=" + maxPages);
    }
}
