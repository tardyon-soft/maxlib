package ru.tardyon.botframework.dispatcher;

import ru.tardyon.botframework.model.Update;

/**
 * Maps incoming updates to observer-level event types.
 *
 * <p>Throwing exception from this contract is treated as
 * {@link RuntimeDispatchErrorType#EVENT_MAPPING_FAILURE} by dispatcher runtime.</p>
 */
public interface UpdateEventResolver {

    UpdateEventResolution resolve(Update update);
}
