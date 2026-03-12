package ru.max.botframework.dispatcher;

import ru.max.botframework.model.Update;

/**
 * Maps incoming updates to observer-level event types.
 */
public interface UpdateEventResolver {

    UpdateEventResolution resolve(Update update);
}

