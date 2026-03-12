package ru.max.botframework.dispatcher;

/**
 * Runtime dispatch-layer error categories.
 */
public enum RuntimeDispatchErrorType {
    HANDLER_FAILURE,
    FILTER_FAILURE,
    OUTER_MIDDLEWARE_FAILURE,
    INNER_MIDDLEWARE_FAILURE,
    ENRICHMENT_FAILURE,
    EVENT_MAPPING_FAILURE,
    OBSERVER_EXECUTION_FAILURE
}
