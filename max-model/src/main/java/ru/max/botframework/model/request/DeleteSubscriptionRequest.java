package ru.max.botframework.model.request;

import java.util.Objects;

/**
 * Typed request for deleting webhook subscription.
 */
public record DeleteSubscriptionRequest(String url) {
    public DeleteSubscriptionRequest {
        Objects.requireNonNull(url, "url");
        if (url.isBlank()) {
            throw new IllegalArgumentException("url must not be blank");
        }
    }
}
