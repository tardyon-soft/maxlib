package ru.max.botframework.client;

import ru.max.botframework.model.Update;

/**
 * Entry point for low-level MAX API operations.
 */
public interface MaxClient {
    void acknowledge(Update update);
}
