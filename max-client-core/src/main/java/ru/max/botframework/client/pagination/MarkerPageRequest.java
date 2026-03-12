package ru.max.botframework.client.pagination;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Marker-based pagination request used by MAX API list endpoints.
 */
public record MarkerPageRequest(int limit, String marker) {
    private static final String DEFAULT_LIMIT_PARAM = "limit";
    private static final String DEFAULT_MARKER_PARAM = "marker";

    public MarkerPageRequest {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        Objects.requireNonNull(marker, "marker");
    }

    public static MarkerPageRequest firstPage(int limit) {
        return new MarkerPageRequest(limit, "");
    }

    public boolean hasMarker() {
        return !marker.isBlank();
    }

    public Map<String, String> toQueryParameters() {
        return toQueryParameters(DEFAULT_LIMIT_PARAM, DEFAULT_MARKER_PARAM);
    }

    public Map<String, String> toQueryParameters(String limitParamName, String markerParamName) {
        Objects.requireNonNull(limitParamName, "limitParamName");
        Objects.requireNonNull(markerParamName, "markerParamName");
        if (limitParamName.isBlank() || markerParamName.isBlank()) {
            throw new IllegalArgumentException("Query parameter names must not be blank");
        }

        Map<String, String> query = new LinkedHashMap<>();
        query.put(limitParamName, String.valueOf(limit));
        if (hasMarker()) {
            query.put(markerParamName, marker);
        }
        return Map.copyOf(query);
    }
}
