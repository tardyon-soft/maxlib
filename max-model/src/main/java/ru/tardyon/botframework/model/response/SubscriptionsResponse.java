package ru.tardyon.botframework.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Objects;
import ru.tardyon.botframework.model.Subscription;

/**
 * Envelope response containing webhook subscriptions list.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SubscriptionsResponse(List<Subscription> subscriptions) {
    public SubscriptionsResponse {
        Objects.requireNonNull(subscriptions, "subscriptions");
        subscriptions = List.copyOf(subscriptions);
    }
}
