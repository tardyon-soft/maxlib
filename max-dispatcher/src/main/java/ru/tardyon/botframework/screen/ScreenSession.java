package ru.tardyon.botframework.screen;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Persisted screen navigation session per resolved runtime scope.
 */
public record ScreenSession(
        String scopeId,
        List<ScreenStackEntry> stack,
        String rootMessageId,
        Instant updatedAt
) {
    public ScreenSession {
        Objects.requireNonNull(scopeId, "scopeId");
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(updatedAt, "updatedAt");
        if (scopeId.isBlank()) {
            throw new IllegalArgumentException("scopeId must not be blank");
        }
        stack = List.copyOf(stack);
    }

    public Optional<ScreenStackEntry> top() {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(stack.get(stack.size() - 1));
    }

    public boolean canGoBack() {
        return stack.size() > 1;
    }
}
