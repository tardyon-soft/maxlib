package ru.max.botframework.filters;

import ru.max.botframework.model.Update;

/**
 * Predicate-like contract for update selection.
 */
@FunctionalInterface
public interface Filter {
    boolean test(Update update);
}
